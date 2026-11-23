package com.semih.ajanofisi.office;

import com.semih.ajanofisi.office.dto.OfficeEventResponse;
import org.springframework.stereotype.Component;

@Component
public class OfficeEventMapper {

    public OfficeEventResponse toResponse(OfficeEvent event) {
        return new OfficeEventResponse(
                event.getId(),
                event.getSessionId(),
                event.getType(),
                event.getAgentId(),
                event.getAgentType(),
                event.getToolName(),
                event.getDetail(),
                event.getCreatedAt());
    }
}
