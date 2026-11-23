package com.semih.ajanofisi.office;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** @param retention how long events are kept before the nightly cleanup deletes them */
@Validated
@ConfigurationProperties(prefix = "app.office")
public record OfficeProperties(@NotNull Duration retention) {
}
