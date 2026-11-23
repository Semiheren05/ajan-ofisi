package com.semih.ajanofisi.office.dto;

import java.time.Instant;

/** A Claude Code session in the list: its project folder name (if known) and activity range. */
public record SessionSummary(String sessionId, String project, Instant startedAt, Instant lastEventAt,
                             Long eventCount) {
}
