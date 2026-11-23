package com.semih.ajanofisi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates Claude Code by the {@code X-Hook-Token} header on {@code /api/hooks}.
 *
 * <p>The client is a program, not a person, so there is no login and no JWT: a long random
 * token in an environment variable is enough. A missing or wrong token does nothing here;
 * the authorization rules in {@link SecurityConfig} turn that into a 401.
 */
public class HookTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Hook-Token";
    public static final String ROLE = "HOOK";

    private final byte[] expected;

    public HookTokenFilter(HookProperties properties) {
        this.expected = properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/hooks");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token != null && matches(token)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "claude-code", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    // Constant-time comparison: the time it takes must not reveal how many characters were right
    private boolean matches(String token) {
        return MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8));
    }
}
