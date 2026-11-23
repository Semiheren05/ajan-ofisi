package com.semih.ajanofisi.common;

/**
 * Small text helpers for the labels shown in the office. Hook payloads can carry whole
 * prompts and long command lines; the page only needs a short, single-line hint.
 */
public final class Texts {

    private static final String ELLIPSIS = "…";

    private Texts() {
        // yardımcı sınıf, nesnesi oluşturulmaz
    }

    /** Collapses whitespace and cuts the text to {@code max} characters; blank input becomes {@code null}. */
    public static String shorten(String text, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String single = text.strip().replaceAll("\\s+", " ");
        if (single.length() <= max) {
            return single;
        }
        return single.substring(0, max - 1).stripTrailing() + ELLIPSIS;
    }

    /**
     * Last segment of a path. Works for both Unix and Windows paths, because Claude Code may run
     * on Windows while this API runs in a Linux container ({@code Path.of} would not split "C:\a\b").
     */
    public static String lastSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.strip().split("[/\\\\]+");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isBlank()) {
                return parts[i];
            }
        }
        return null;
    }
}
