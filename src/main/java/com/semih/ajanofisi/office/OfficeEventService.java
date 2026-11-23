package com.semih.ajanofisi.office;

import com.semih.ajanofisi.common.Texts;
import com.semih.ajanofisi.common.exception.ResourceNotFoundException;
import com.semih.ajanofisi.hook.dto.HookPayload;
import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import com.semih.ajanofisi.office.dto.SessionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Turns hook payloads into office events, stores them and announces them to the live stream.
 *
 * <p>Only short labels are stored (a file name, a command description, the first words of a
 * prompt), never file contents or whole prompts: the office needs a hint, not a transcript.
 */
@Service
public class OfficeEventService {

    private static final Logger log = LoggerFactory.getLogger(OfficeEventService.class);

    static final int ID_LENGTH = 100;
    static final int PROMPT_LENGTH = 120;
    static final int MESSAGE_LENGTH = 160;

    private final OfficeEventRepository repository;
    private final OfficeEventMapper mapper;
    private final ToolSummarizer toolSummarizer;
    private final ApplicationEventPublisher events;
    private final OfficeProperties properties;
    private final Clock clock;

    public OfficeEventService(OfficeEventRepository repository, OfficeEventMapper mapper,
                              ToolSummarizer toolSummarizer, ApplicationEventPublisher events,
                              OfficeProperties properties, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.toolSummarizer = toolSummarizer;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    /** Records the payload if the office cares about its hook event; returns empty otherwise. */
    @Transactional
    public Optional<OfficeEventResponse> record(HookPayload payload) {
        Optional<OfficeEventType> type = OfficeEventType.fromHook(payload.hookEventName());
        if (type.isEmpty()) {
            log.debug("Ignoring hook event hookEventName={}", payload.hookEventName());
            return Optional.empty();
        }

        OfficeEvent event = new OfficeEvent(
                payload.sessionId(),
                type.get(),
                Texts.shorten(payload.agentId(), ID_LENGTH),
                Texts.shorten(payload.agentType(), ID_LENGTH),
                type.get() == OfficeEventType.TOOL ? Texts.shorten(payload.toolName(), ID_LENGTH) : null,
                detail(type.get(), payload),
                clock.instant());
        OfficeEventResponse saved = mapper.toResponse(repository.save(event));
        events.publishEvent(new OfficeEventRecorded(saved));
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Page<SessionSummary> sessions(Pageable pageable) {
        // The query sorts by last activity itself; a client-supplied sort would clash with GROUP BY
        return repository.findSessions(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }

    /** The session's events in time order (at most the newest 500). */
    @Transactional(readOnly = true)
    public List<OfficeEventResponse> events(String sessionId) {
        List<OfficeEvent> newestFirst = repository.findTop500BySessionIdOrderByIdDesc(sessionId);
        if (newestFirst.isEmpty()) {
            throw new ResourceNotFoundException("Session '" + sessionId + "' not found");
        }
        return newestFirst.reversed().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public int purgeExpired() {
        Instant before = clock.instant().minus(properties.retention());
        int deleted = repository.deleteOlderThan(before);
        log.info("Deleted old office events count={} before={}", deleted, before);
        return deleted;
    }

    private String detail(OfficeEventType type, HookPayload payload) {
        return switch (type) {
            case SESSION_STARTED -> Texts.lastSegment(payload.cwd());
            case PROMPT -> Texts.shorten(payload.prompt(), PROMPT_LENGTH);
            case TOOL -> toolSummarizer.target(payload.toolName(), payload.toolInput());
            case AGENT_STOPPED, STOPPED -> Texts.shorten(payload.lastAssistantMessage(), MESSAGE_LENGTH);
            case WAITING -> Texts.shorten(payload.message(), MESSAGE_LENGTH);
            case AGENT_STARTED, SESSION_ENDED -> null;
        };
    }
}
