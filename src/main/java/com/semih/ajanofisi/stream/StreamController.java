package com.semih.ajanofisi.stream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
@Tag(name = "Live stream")
public class StreamController {

    private final OfficeStream officeStream;

    public StreamController(OfficeStream officeStream) {
        this.officeStream = officeStream;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Server-Sent Events: one 'office-event' per new event")
    public ResponseEntity<SseEmitter> stream() {
        return ResponseEntity.ok()
                // Nginx would otherwise buffer the stream and deliver events in batches
                .header("X-Accel-Buffering", "no")
                .body(officeStream.subscribe());
    }
}
