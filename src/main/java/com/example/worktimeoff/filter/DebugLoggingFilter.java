package com.example.worktimeoff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DebugLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // only log the auth login POST to avoid noisy logs
        return !"/api/auth/login".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : null);
            info.put("requestedSessionId", request.getRequestedSessionId());
            String header = request.getHeader("X-XSRF-TOKEN");
            if (header == null) header = request.getHeader("x-xsrf-token");
            info.put("header_X_XSRF_TOKEN", header);
            Map<String, String> cookies = new HashMap<>();
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("JSESSIONID".equalsIgnoreCase(c.getName()) || "XSRF-TOKEN".equalsIgnoreCase(c.getName())) {
                        cookies.put(c.getName(), c.getValue());
                    }
                }
            }
            info.put("cookies", cookies);

            // Also load token via CookieCsrfTokenRepository so we see what the server-side repo returns for this request
            CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
            CsrfToken token = repo.loadToken(request);
            info.put("csrfRepoToken", token != null ? token.getToken() : null);

            log.info("[DEBUG-LOGIN-REQUEST] {}", info);
        } catch (Exception ex) {
            log.warn("Failed to log debug login request", ex);
        }
        filterChain.doFilter(request, response);
    }
}
