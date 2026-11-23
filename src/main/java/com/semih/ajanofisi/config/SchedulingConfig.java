package com.semih.ajanofisi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled}: the stream heartbeat and the nightly cleanup of old events. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {
}
