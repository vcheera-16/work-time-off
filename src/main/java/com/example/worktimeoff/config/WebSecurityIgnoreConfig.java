package com.example.worktimeoff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class WebSecurityIgnoreConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // WARNING: This ignores ALL requests from the security filter chain.
        // This is intended as a temporary development/debugging helper only.
        return (web) -> web.ignoring().requestMatchers(
            new AntPathRequestMatcher("/**")
        );
    }
}
