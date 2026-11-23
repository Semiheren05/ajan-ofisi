package com.semih.ajanofisi.hook;

import com.semih.ajanofisi.config.OpenApiConfig;
import com.semih.ajanofisi.hook.dto.HookAck;
import com.semih.ajanofisi.hook.dto.HookPayload;
import com.semih.ajanofisi.office.OfficeEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hooks")
@Tag(name = "Hooks")
public class HookController {

    private final OfficeEventService officeEventService;

    public HookController(OfficeEventService officeEventService) {
        this.officeEventService = officeEventService;
    }

    @PostMapping
    @Operation(summary = "Receive a Claude Code hook event (configured as an HTTP hook)")
    @SecurityRequirement(name = OpenApiConfig.HOOK_TOKEN_SCHEME)
    public ResponseEntity<HookAck> receive(@Valid @RequestBody HookPayload payload) {
        officeEventService.record(payload);
        return ResponseEntity.ok(HookAck.CONTINUE);
    }
}
