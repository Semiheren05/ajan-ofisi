package com.semih.ajanofisi.stream;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param timeout   how long one SSE connection stays open; the browser reconnects by itself afterwards
 * @param heartbeat how often an empty comment is sent, so proxies do not close an idle connection
 */
@Validated
@ConfigurationProperties(prefix = "app.stream")
public record StreamProperties(@NotNull Duration timeout, @NotNull Duration heartbeat) {
}
