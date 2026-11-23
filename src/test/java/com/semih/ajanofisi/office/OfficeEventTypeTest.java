package com.semih.ajanofisi.office;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeEventTypeTest {

    @ParameterizedTest
    @CsvSource({
            "SessionStart, SESSION_STARTED",
            "UserPromptSubmit, PROMPT",
            "PreToolUse, TOOL",
            "SubagentStart, AGENT_STARTED",
            "SubagentStop, AGENT_STOPPED",
            "Notification, WAITING",
            "Stop, STOPPED",
            "SessionEnd, SESSION_ENDED"
    })
    void everyDrawnHookHasAnEventType(String hookEventName, OfficeEventType expected) {
        assertThat(OfficeEventType.fromHook(hookEventName)).contains(expected);
    }

    @Test
    void hooksTheOfficeDoesNotDrawHaveNoType() {
        assertThat(OfficeEventType.fromHook("PreCompact")).isEmpty();
        assertThat(OfficeEventType.fromHook("pretooluse")).isEmpty();
        assertThat(OfficeEventType.fromHook(null)).isEmpty();
    }
}
