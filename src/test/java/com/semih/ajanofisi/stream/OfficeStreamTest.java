package com.semih.ajanofisi.stream;

import com.semih.ajanofisi.office.OfficeEventType;
import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeStreamTest {

    private static final OfficeEventResponse EVENT = new OfficeEventResponse(1L, "s-1", OfficeEventType.TOOL,
            "agent-1", "Explore", "Read", "UserService.java", Instant.parse("2026-11-23T10:00:00Z"));

    private final List<CountingEmitter> created = new ArrayList<>();
    private final OfficeStream stream = new OfficeStream(new StreamProperties(Duration.ofMinutes(30),
            Duration.ofHours(1))) {
        @Override
        protected SseEmitter newEmitter(long timeoutMillis) {
            CountingEmitter emitter = new CountingEmitter(timeoutMillis);
            created.add(emitter);
            return emitter;
        }
    };

    @Test
    void subscribeKeepsTheConnectionAndGreetsIt() {
        stream.subscribe();

        assertThat(stream.subscriberCount()).isEqualTo(1);
        assertThat(created.getFirst().sent).isEqualTo(1);
    }

    @Test
    void publishReachesEveryConnection() {
        stream.subscribe();
        stream.subscribe();

        stream.publish(EVENT);

        assertThat(created).extracting(emitter -> emitter.sent).containsExactly(2, 2);
    }

    @Test
    void aClosedConnectionIsDroppedAndTheOthersStillGetTheEvent() {
        stream.subscribe();
        stream.subscribe();
        created.getFirst().broken = true;

        stream.publish(EVENT);

        assertThat(stream.subscriberCount()).isEqualTo(1);
        assertThat(created.get(1).sent).isEqualTo(2);
    }

    /** Counts writes; once "broken" it fails like a connection whose browser tab was closed. */
    private static final class CountingEmitter extends SseEmitter {

        private int sent;
        private boolean broken;

        private CountingEmitter(long timeoutMillis) {
            super(timeoutMillis);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (broken) {
                throw new IOException("Broken pipe");
            }
            sent++;
        }
    }
}
