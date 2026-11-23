package com.semih.ajanofisi.office;

import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import com.semih.ajanofisi.office.dto.SessionSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Sessions")
public class OfficeController {

    private final OfficeEventService officeEventService;

    public OfficeController(OfficeEventService officeEventService) {
        this.officeEventService = officeEventService;
    }

    @GetMapping
    @Operation(summary = "Claude Code sessions, most recently active first")
    public ResponseEntity<Page<SessionSummary>> sessions(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(officeEventService.sessions(pageable));
    }

    @GetMapping("/{sessionId}/events")
    @Operation(summary = "A session's events in time order (newest 500), to rebuild the office")
    public ResponseEntity<List<OfficeEventResponse>> events(@PathVariable String sessionId) {
        return ResponseEntity.ok(officeEventService.events(sessionId));
    }
}
