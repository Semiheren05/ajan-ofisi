package com.semih.ajanofisi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Receives Claude Code hook events, stores them and streams them to a pixel-art office page.
 *
 * <p>The default in-memory user is turned off: the only authenticated client is Claude Code
 * itself, which sends a shared token (see {@code HookTokenFilter}).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class AjanOfisiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AjanOfisiApplication.class, args);
    }
}
