package com.semih.ajanofisi.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Shared secret Claude Code sends in the {@code X-Hook-Token} header.
 *
 * @param token at least 16 characters; set {@code AJAN_OFISI_TOKEN} outside local development
 */
@Validated
@ConfigurationProperties(prefix = "app.hook")
public record HookProperties(@NotBlank @Size(min = 16) String token) {
}
