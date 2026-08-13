package com.example.worktimeoff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class WebSecurityIgnoreConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Ignore only static assets and the H2 console so the security filter chain still
        // runs for API endpoints (including /api/csrf) and CSRF tokens can be generated.
        return (web) -> web.ignoring().requestMatchers(
            new AntPathRequestMatcher("/h2-console/**"),
            new AntPathRequestMatcher("/main.js"),
            new AntPathRequestMatcher("/**/*.js"),
            new AntPathRequestMatcher("/**/*.css"),
            new AntPathRequestMatcher("/**/*.map"),
            new AntPathRequestMatcher("/favicon.ico")
        );
    }
}
