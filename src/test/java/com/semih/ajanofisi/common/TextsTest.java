package com.semih.ajanofisi.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextsTest {

    @Test
    void shortenPutsEverythingOnOneLine() {
        assertThat(Texts.shorten("  JWT ile\n giriş   ekle  ", 50)).isEqualTo("JWT ile giriş ekle");
    }

    @Test
    void shortenCutsLongTextWithAnEllipsis() {
        String shortened = Texts.shorten("abcdefghij", 5);

        assertThat(shortened).isEqualTo("abcd…").hasSize(5);
    }

    @Test
    void shortenKeepsTextThatAlreadyFits() {
        assertThat(Texts.shorten("abcde", 5)).isEqualTo("abcde");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void shortenTurnsBlankTextIntoNull(String text) {
        assertThat(Texts.shorten(text, 10)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/semih/randevu-api", "/home/semih/randevu-api/", "C:\\Users\\semih\\randevu-api",
            "C:\\Users\\semih\\randevu-api\\", "randevu-api"})
    void lastSegmentWorksForUnixAndWindowsPaths(String path) {
        assertThat(Texts.lastSegment(path)).isEqualTo("randevu-api");
    }

    @Test
    void lastSegmentOfNothingIsNull() {
        assertThat(Texts.lastSegment(null)).isNull();
        assertThat(Texts.lastSegment("/")).isNull();
    }
}
