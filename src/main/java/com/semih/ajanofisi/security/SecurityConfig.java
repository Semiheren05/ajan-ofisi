package com.semih.ajanofisi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

/**
 * Two kinds of clients:
 * <ul>
 *   <li>Claude Code posts hook events to {@code /api/hooks}: needs the hook token.</li>
 *   <li>The office page reads sessions, events and the live stream: open, because the app
 *       is meant to run on your own machine (docker-compose publishes it on 127.0.0.1 only).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, HookProperties hookProperties,
                                            ObjectMapper objectMapper, Clock clock) throws Exception {
        var problemHandler = new ProblemDetailSecurityHandler(objectMapper, clock);

        http
                // No cookies and no browser session: the token travels in a header that browsers never
                // send on their own, so there is nothing for a CSRF attack to ride on.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(new HookTokenFilter(hookProperties), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/hooks", "/api/hooks/**").hasRole(HookTokenFilter.ROLE)
                        .anyRequest().permitAll());
        return http.build();
    }
}
