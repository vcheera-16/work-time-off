package com.example.worktimeoff.controller;

import com.example.worktimeoff.dto.AuthRequest;
import com.example.worktimeoff.dto.AuthResponse;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.security.LoginAttemptService;
import com.example.worktimeoff.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest req, BindingResult binding) {
        if (binding.hasErrors()) {
            return ResponseEntity.badRequest().body(binding.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList()));
        }
        if (userService.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already registered");
        }
        User u = userService.registerUser(req.getEmail(), req.getPassword(), null);
        return ResponseEntity.ok(new AuthResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req, BindingResult binding, HttpServletRequest request) {
        if (binding.hasErrors()) {
            return ResponseEntity.badRequest().body(binding.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList()));
        }
        String clientKey = getClientKey(request, req.getEmail());
        if (loginAttemptService.isBlocked(clientKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("too many failed login attempts, try later");
        }
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            // create session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            // reset attempts
            loginAttemptService.reset(clientKey);
            // return basic user info
            User u = userService.findByEmail(req.getEmail()).orElseThrow();
            return ResponseEntity.ok(new AuthResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole()));
        } catch (Exception ex) {
            loginAttemptService.recordFailure(clientKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }
    }

    private String getClientKey(HttpServletRequest req, String email) {
        String ip = req.getRemoteAddr();
        return ip + ":" + (email == null ? "" : email.toLowerCase());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) s.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(new AuthResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole()));
    }
}
