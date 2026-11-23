package com.semih.ajanofisi.office;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolSummarizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolSummarizer summarizer = new ToolSummarizer();

    @Test
    void readShowsOnlyTheFileName() {
        JsonNode input = input(Map.of("file_path", "/home/semih/randevu-api/src/BookingService.java"));

        assertThat(summarizer.target("Read", input)).isEqualTo("BookingService.java");
    }

    @Test
    void editNeverShowsTheChangedCode() {
        JsonNode input = input(Map.of("file_path", "/a/SecurityConfig.java",
                "old_string", "secret()", "new_string", "other()"));

        assertThat(summarizer.target("Edit", input)).isEqualTo("SecurityConfig.java");
    }

    @Test
    void bashPrefersTheDescription() {
        JsonNode input = input(Map.of("command", "mvn -B -q test", "description", "Testleri çalıştır"));

        assertThat(summarizer.target("Bash", input)).isEqualTo("Testleri çalıştır");
    }

    @Test
    void bashFallsBackToTheCommand() {
        assertThat(summarizer.target("Bash", input(Map.of("command", "git status")))).isEqualTo("git status");
    }

    @Test
    void grepShowsThePattern() {
        assertThat(summarizer.target("Grep", input(Map.of("pattern", "SecurityFilterChain"))))
                .isEqualTo("SecurityFilterChain");
    }

    @Test
    void webFetchShowsOnlyTheHost() {
        JsonNode input = input(Map.of("url", "https://docs.spring.io/spring-boot/index.html?token=abc"));

        assertThat(summarizer.target("WebFetch", input)).isEqualTo("docs.spring.io");
    }

    @Test
    void agentShowsTheTaskDescription() {
        JsonNode input = input(Map.of("description", "Kod tabanını tara", "subagent_type", "Explore",
                "prompt", "a long prompt"));

        assertThat(summarizer.target("Agent", input)).isEqualTo("Kod tabanını tara");
    }

    @Test
    void longCommandsAreShortened() {
        String target = summarizer.target("Bash", input(Map.of("command", "echo " + "x".repeat(200))));

        assertThat(target).hasSize(ToolSummarizer.MAX_LENGTH).endsWith("…");
    }

    @Test
    void unknownToolsAndMissingInputHaveNoTarget() {
        assertThat(summarizer.target("mcp__github__create_issue", input(Map.of("title", "x")))).isNull();
        assertThat(summarizer.target("Read", null)).isNull();
        assertThat(summarizer.target(null, input(Map.of("file_path", "/a/b")))).isNull();
    }

    @Test
    void aFieldWithTheWrongTypeIsIgnored() {
        assertThat(summarizer.target("Read", input(Map.of("file_path", 42)))).isNull();
    }

    private JsonNode input(Map<String, ?> fields) {
        return objectMapper.valueToTree(fields);
    }
}
