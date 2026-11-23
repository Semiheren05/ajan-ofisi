package com.semih.ajanofisi.hook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hook response in Claude Code's output format. {@code {"continue": true}} means "carry on as
 * usual": the office only watches, it never blocks or changes what the agent does.
 */
public record HookAck(@JsonProperty("continue") boolean proceed) {

    public static final HookAck CONTINUE = new HookAck(true);
}
