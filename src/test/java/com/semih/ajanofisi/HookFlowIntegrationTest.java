package com.semih.ajanofisi;

import com.semih.ajanofisi.office.OfficeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Claude Code posts hook events, the office reads them back: end to end against PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class HookFlowIntegrationTest {

    private static final String TOKEN = "test-hook-token-1234567890";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OfficeEventRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void hookWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hook("s-1", "SessionStart", "")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    void hookWithWrongTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/hooks")
                        .header("X-Hook-Token", "wrong-token-1234567890")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hook("s-1", "SessionStart", "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hookWithoutEventNameIsAValidationError() throws Exception {
        sendHook("""
                {"session_id": "s-1"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.hookEventName").exists());
    }

    @Test
    void subagentWorkIsStoredAndReadBackInOrder() throws Exception {
        sendHook(hook("s-1", "SessionStart", ", \"cwd\": \"/home/semih/randevu-api\", \"source\": \"startup\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.continue").value(true));
        sendHook(hook("s-1", "SubagentStart", ", \"agent_id\": \"ag-1\", \"agent_type\": \"Explore\""));
        sendHook(hook("s-1", "PreToolUse", """
                , "agent_id": "ag-1", "agent_type": "Explore", "tool_name": "Read", "tool_use_id": "toolu_1",
                "tool_input": {"file_path": "/home/semih/randevu-api/BookingService.java"}"""));
        sendHook(hook("s-1", "SubagentStop", """
                , "agent_id": "ag-1", "agent_type": "Explore", "stop_hook_active": false,
                "last_assistant_message": "3 dosya bulundu", "background_tasks": []"""));

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sessionId").value("s-1"))
                .andExpect(jsonPath("$.content[0].project").value("randevu-api"))
                .andExpect(jsonPath("$.content[0].eventCount").value(4))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/sessions/s-1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].type").value("SESSION_STARTED"))
                .andExpect(jsonPath("$[1].type").value("AGENT_STARTED"))
                .andExpect(jsonPath("$[2].type").value("TOOL"))
                .andExpect(jsonPath("$[2].agentId").value("ag-1"))
                .andExpect(jsonPath("$[2].toolName").value("Read"))
                .andExpect(jsonPath("$[2].detail").value("BookingService.java"))
                .andExpect(jsonPath("$[3].type").value("AGENT_STOPPED"))
                .andExpect(jsonPath("$[3].detail").value("3 dosya bulundu"));
    }

    @Test
    void sessionsAreListedByLastActivity() throws Exception {
        sendHook(hook("older", "SessionStart", ""));
        sendHook(hook("newer", "SessionStart", ""));
        sendHook(hook("older", "Stop", ""));

        mockMvc.perform(get("/api/sessions"))
                .andExpect(jsonPath("$.content[0].sessionId").value("older"))
                .andExpect(jsonPath("$.content[1].sessionId").value("newer"));
    }

    @Test
    void hookEventsTheOfficeDoesNotDrawAreAcceptedButNotStored() throws Exception {
        sendHook(hook("s-1", "PreCompact", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.continue").value(true));

        assertThat(repository.count()).isZero();
    }

    @Test
    void unknownSessionIsNotFound() throws Exception {
        mockMvc.perform(get("/api/sessions/nope/events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void streamOpensAServerSentEventsConnection() throws Exception {
        mockMvc.perform(get("/api/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void officePageAndIconsAreServed() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mockMvc.perform(get("/favicon-48.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        mockMvc.perform(get("/llms.txt"))
                .andExpect(status().isOk());
    }

    private ResultActions sendHook(String json) throws Exception {
        return mockMvc.perform(post("/api/hooks")
                .header("X-Hook-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** A Claude Code payload with the common fields; {@code extra} starts with a comma. */
    private static String hook(String sessionId, String event, String extra) {
        return """
                {"session_id": "%s", "hook_event_name": "%s", "transcript_path": "/tmp/t.jsonl",
                 "permission_mode": "default"%s}
                """.formatted(sessionId, event, extra);
    }
}
