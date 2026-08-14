package com.example.worktimeoff.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    private final CsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();

    @GetMapping("/debug/csrf-info")
    public Map<String, Object> csrfInfo(HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();
        HttpSession s = request.getSession(false);
        out.put("sessionPresent", s != null);
        out.put("sessionId", s != null ? s.getId() : null);
        out.put("requestedSessionId", request.getRequestedSessionId());

        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                // include only relevant cookies to avoid leaking unrelated data
                if ("JSESSIONID".equalsIgnoreCase(c.getName()) || "XSRF-TOKEN".equalsIgnoreCase(c.getName())) {
                    cookies.put(c.getName(), c.getValue());
                }
            }
        }
        out.put("cookies", cookies);

        String header = request.getHeader("X-XSRF-TOKEN");
        out.put("header_X_XSRF_TOKEN", header);

        // use the configured repo to load token as the server sees it
        CsrfToken token = csrfRepo.loadToken(request);
        out.put("csrfRepoToken", token != null ? token.getToken() : null);

        return out;
    }
}
