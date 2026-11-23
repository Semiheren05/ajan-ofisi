package com.semih.ajanofisi.office;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semih.ajanofisi.common.exception.ResourceNotFoundException;
import com.semih.ajanofisi.hook.dto.HookPayload;
import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    @Mock
    private OfficeEventRepository repository;
    @Mock
    private ApplicationEventPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OfficeEventService service;

    @BeforeEach
    void setUp() {
        service = new OfficeEventService(repository, new OfficeEventMapper(), new ToolSummarizer(), publisher,
                new OfficeProperties(Duration.ofDays(7)), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsASubagentStartAndAnnouncesIt() {
        saveReturnsTheEntity();

        Optional<OfficeEventResponse> result = service.record(
                payload("SubagentStart", "agent-1", "Explore", null, null));

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(OfficeEventType.AGENT_STARTED);
        assertThat(result.get().agentId()).isEqualTo("agent-1");
        assertThat(result.get().agentType()).isEqualTo("Explore");
        assertThat(result.get().createdAt()).isEqualTo(NOW);
        ArgumentCaptor<OfficeEventRecorded> announced = ArgumentCaptor.forClass(OfficeEventRecorded.class);
        verify(publisher).publishEvent(announced.capture());
        assertThat(announced.getValue().event()).isEqualTo(result.get());
    }

    @Test
    void toolEventKeepsOnlyTheToolAndItsTarget() {
        saveReturnsTheEntity();
        JsonNode input = objectMapper.valueToTree(Map.of(
                "file_path", "/home/semih/site-api/UserService.java", "content", "class UserService {}"));

        OfficeEventResponse event = service.record(payload("PreToolUse", "agent-1", "Explore", "Write", input))
                .orElseThrow();

        assertThat(event.toolName()).isEqualTo("Write");
        assertThat(event.detail()).isEqualTo("UserService.java");
    }

    @Test
    void promptIsShortened() {
        saveReturnsTheEntity();
        HookPayload payload = new HookPayload("s-1", "UserPromptSubmit", null, null, null, null,
                "a".repeat(300), null, null, null);

        OfficeEventResponse event = service.record(payload).orElseThrow();

        assertThat(event.detail()).hasSize(OfficeEventService.PROMPT_LENGTH);
        assertThat(event.toolName()).isNull();
    }

    @Test
    void sessionStartRemembersTheProjectFolder() {
        saveReturnsTheEntity();
        HookPayload payload = new HookPayload("s-1", "SessionStart", null, null, null, null,
                null, null, "/home/semih/randevu-api", null);

        assertThat(service.record(payload).orElseThrow().detail()).isEqualTo("randevu-api");
    }

    @Test
    void veryLongAgentNamesAreShortenedInsteadOfRejected() {
        saveReturnsTheEntity();

        OfficeEventResponse event = service.record(payload("SubagentStart", "agent-1", "p".repeat(150), null, null))
                .orElseThrow();

        assertThat(event.agentType()).hasSize(OfficeEventService.ID_LENGTH);
    }

    @Test
    void ignoresHookEventsTheOfficeDoesNotDraw() {
        Optional<OfficeEventResponse> result = service.record(payload("PreCompact", null, null, null, null));

        assertThat(result).isEmpty();
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void eventsComeBackInTimeOrder() {
        OfficeEvent first = event("s-1", "first");
        OfficeEvent second = event("s-1", "second");
        when(repository.findTop500BySessionIdOrderByIdDesc("s-1")).thenReturn(List.of(second, first));

        List<OfficeEventResponse> events = service.events("s-1");

        assertThat(events).extracting(OfficeEventResponse::detail).containsExactly("first", "second");
    }

    @Test
    void eventsOfAnUnknownSessionAreNotFound() {
        when(repository.findTop500BySessionIdOrderByIdDesc("nope")).thenReturn(List.of());

        assertThatThrownBy(() -> service.events("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void purgeDeletesEventsOlderThanTheRetention() {
        when(repository.deleteOlderThan(NOW.minus(Duration.ofDays(7)))).thenReturn(3);

        int deleted = service.purgeExpired();

        assertThat(deleted).isEqualTo(3);
        verify(repository).deleteOlderThan(NOW.minus(Duration.ofDays(7)));
    }

    private void saveReturnsTheEntity() {
        when(repository.save(any(OfficeEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static HookPayload payload(String hook, String agentId, String agentType, String toolName,
                                       JsonNode toolInput) {
        return new HookPayload("s-1", hook, agentId, agentType, toolName, toolInput, null, null, null, null);
    }

    private static OfficeEvent event(String sessionId, String detail) {
        return new OfficeEvent(sessionId, OfficeEventType.TOOL, null, null, "Read", detail, NOW);
    }
}
