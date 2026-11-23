package com.semih.ajanofisi.hook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The JSON Claude Code posts for every hook event (snake_case, as Claude Code sends it).
 *
 * <p>Only the fields the office needs are mapped; unknown fields are ignored so new Claude Code
 * versions can add fields without breaking this API. Optional fields are not size-limited here:
 * a long plugin agent name must not turn into a 400, the service shortens them instead.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HookPayload(
        @JsonProperty("session_id") @NotBlank @Size(max = 100) String sessionId,
        @JsonProperty("hook_event_name") @NotBlank @Size(max = 50) String hookEventName,
        @JsonProperty("agent_id") String agentId,
        @JsonProperty("agent_type") String agentType,
        @JsonProperty("tool_name") String toolName,
        @JsonProperty("tool_input") JsonNode toolInput,
        @JsonProperty("prompt") String prompt,
        @JsonProperty("message") String message,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("last_assistant_message") String lastAssistantMessage) {
}
