package com.semih.ajanofisi.stream;

import com.semih.ajanofisi.office.OfficeEventRecorded;
import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps the open browser connections and pushes every new office event to all of them
 * (Server-Sent Events).
 *
 * <p>SSE instead of WebSocket: the data only flows one way (server to page), SSE runs over plain
 * HTTP and the browser's {@code EventSource} reconnects by itself. Events are sent only after the
 * database transaction commits, so the page never shows an event that was rolled back.
 */
@Component
public class OfficeStream {

    private static final Logger log = LoggerFactory.getLogger(OfficeStream.class);

    static final String EVENT_NAME = "office-event";

    // Copy-on-write: sends iterate often, subscribers change rarely, and no lock is held while sending
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final StreamProperties properties;

    public OfficeStream(StreamProperties properties) {
        this.properties = properties;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = newEmitter(properties.timeout().toMillis());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));
        send(emitter, SseEmitter.event().comment("connected"));
        log.debug("Stream subscriber added subscribers={}", emitters.size());
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecorded(OfficeEventRecorded recorded) {
        publish(recorded.event());
    }

    public void publish(OfficeEventResponse event) {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event()
                    .id(String.valueOf(event.id()))
                    .name(EVENT_NAME)
                    .data(event));
        }
    }

    @Scheduled(fixedRateString = "${app.stream.heartbeat}")
    void heartbeat() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().comment("ping"));
        }
    }

    public int subscriberCount() {
        return emitters.size();
    }

    /** Separate method so a test can hand in an emitter whose connection is already gone. */
    protected SseEmitter newEmitter(long timeoutMillis) {
        return new SseEmitter(timeoutMillis);
    }

    // A closed tab shows up here as a failed write: forget that connection and carry on with the others
    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
            log.debug("Stream subscriber dropped subscribers={}", emitters.size());
        }
    }
}
