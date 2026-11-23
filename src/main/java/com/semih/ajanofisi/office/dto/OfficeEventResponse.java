package com.semih.ajanofisi.office.dto;

import com.semih.ajanofisi.office.OfficeEventType;

import java.time.Instant;

/**
 * An office event as the page sees it, both in the history and in the live stream.
 * {@code agentId} is empty for the main agent ("the boss").
 */
public record OfficeEventResponse(Long id, String sessionId, OfficeEventType type, String agentId,
                                  String agentType, String toolName, String detail, Instant createdAt) {
}
