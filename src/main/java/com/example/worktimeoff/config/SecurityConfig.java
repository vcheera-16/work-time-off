package com.example.worktimeoff.config;

import com.example.worktimeoff.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authProvider) {
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .authorizeHttpRequests(authorize -> authorize
                // Permit GET requests for static resources and h2 console explicitly using AntPathRequestMatcher with method
                .requestMatchers(
                    new AntPathRequestMatcher("/", "GET"),
                    new AntPathRequestMatcher("/index.html", "GET"),
                    new AntPathRequestMatcher("/static/**", "GET"),
                    new AntPathRequestMatcher("/**/*.css", "GET"),
                    new AntPathRequestMatcher("/**/*.js", "GET"),
                    new AntPathRequestMatcher("/**/*.map", "GET"),
                    new AntPathRequestMatcher("/**/*.html", "GET"),
                    new AntPathRequestMatcher("/favicon.ico", "GET"),
                    new AntPathRequestMatcher("/h2-console/**", "GET"),
                    new AntPathRequestMatcher("/api/csrf", "GET"),
                    new AntPathRequestMatcher("/api/auth/**")
                ).permitAll()
                .anyRequest().authenticated()
            )
            // allow frames from same origin so H2 console can render
            .headers(headers -> headers.frameOptions().sameOrigin())
            .formLogin().disable()
            .httpBasic().disable();

        return http.build();
    }
}
