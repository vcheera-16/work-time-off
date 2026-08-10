package com.example.worktimeoff.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    @GetMapping
    public Map<String, String> csrf(CsrfToken token, HttpServletRequest request) {
        // This endpoint ensures a CSRF token is generated and returned to the client.
        return Map.of("token", token.getToken());
    }
}
