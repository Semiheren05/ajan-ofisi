package com.semih.ajanofisi.office;

import com.fasterxml.jackson.databind.JsonNode;
import com.semih.ajanofisi.common.Texts;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Picks the one thing worth showing in a speech bubble from a tool call: the file name for
 * Read/Edit/Write, the description (or the command) for Bash, the pattern for Grep...
 *
 * <p>Only the target is returned ("UserService.java"); the page turns the tool name into a verb
 * ("Okuyor"), so the API stays language-neutral. Whole file contents and edit diffs in
 * {@code tool_input} are never stored.
 */
@Component
public class ToolSummarizer {

    static final int MAX_LENGTH = 80;

    public String target(String toolName, JsonNode input) {
        if (toolName == null || input == null || !input.isObject()) {
            return null;
        }
        String target = switch (toolName) {
            case "Read", "Edit", "MultiEdit", "Write" -> Texts.lastSegment(text(input, "file_path"));
            case "NotebookEdit" -> Texts.lastSegment(text(input, "notebook_path"));
            case "Bash" -> firstPresent(text(input, "description"), text(input, "command"));
            case "Grep", "Glob" -> text(input, "pattern");
            case "WebSearch" -> text(input, "query");
            case "WebFetch" -> host(text(input, "url"));
            case "Agent", "Task" -> firstPresent(text(input, "description"), text(input, "subagent_type"));
            default -> null;
        };
        return Texts.shorten(target, MAX_LENGTH);
    }

    private static String text(JsonNode input, String field) {
        JsonNode value = input.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static String firstPresent(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    // Only the host: query strings can carry tokens
    private static String host(String url) {
        if (url == null) {
            return null;
        }
        try {
            String host = new URI(url.strip()).getHost();
            return host != null ? host : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
