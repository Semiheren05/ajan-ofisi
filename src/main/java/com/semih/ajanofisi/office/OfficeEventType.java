package com.semih.ajanofisi.office;

import java.util.Arrays;
import java.util.Optional;

/**
 * What happens in the office, and which Claude Code hook causes it.
 *
 * <p>Claude Code has many more hook events (PreCompact, ConfigChange...). The office only
 * listens to the ones it can draw; the rest are accepted and ignored.
 */
public enum OfficeEventType {

    /** The lights go on. */
    SESSION_STARTED("SessionStart"),
    /** The boss gets a new task. */
    PROMPT("UserPromptSubmit"),
    /** Someone (the boss or a subagent) uses a tool: reads a file, runs a command... */
    TOOL("PreToolUse"),
    /** A subagent walks in and takes a desk. */
    AGENT_STARTED("SubagentStart"),
    /** A subagent finishes, grabs a coffee and leaves. */
    AGENT_STOPPED("SubagentStop"),
    /** Claude Code is waiting for you (permission request, idle prompt). */
    WAITING("Notification"),
    /** The boss finished the answer. */
    STOPPED("Stop"),
    /** The lights go off. */
    SESSION_ENDED("SessionEnd");

    private final String hookEventName;

    OfficeEventType(String hookEventName) {
        this.hookEventName = hookEventName;
    }

    public static Optional<OfficeEventType> fromHook(String hookEventName) {
        return Arrays.stream(values())
                .filter(type -> type.hookEventName.equals(hookEventName))
                .findFirst();
    }

    public String getHookEventName() {
        return hookEventName;
    }
}
