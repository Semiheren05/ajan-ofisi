package com.semih.ajanofisi.office;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One thing that happened in the office. Events are append-only: they are never edited,
 * only deleted when they are older than the retention period.
 */
@Entity
@Table(name = "office_events")
public class OfficeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OfficeEventType type;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "agent_type", length = 100)
    private String agentType;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    protected OfficeEvent() {
        // for JPA
    }

    public OfficeEvent(String sessionId, OfficeEventType type, String agentId, String agentType,
                       String toolName, String detail, Instant createdAt) {
        this.sessionId = sessionId;
        this.type = type;
        this.agentId = agentId;
        this.agentType = agentType;
        this.toolName = toolName;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public OfficeEventType getType() {
        return type;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getToolName() {
        return toolName;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
