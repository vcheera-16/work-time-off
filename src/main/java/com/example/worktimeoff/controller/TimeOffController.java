package com.example.worktimeoff.controller;

import com.example.worktimeoff.dto.CreateTimeOffRequest;
import com.example.worktimeoff.model.TimeOffRequest;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.service.TimeOffService;
import com.example.worktimeoff.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/timeoff")
public class TimeOffController {

    private final TimeOffService timeOffService;
    private final UserService userService;

    public TimeOffController(TimeOffService timeOffService, UserService userService) {
        this.timeOffService = timeOffService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTimeOffRequest req, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        try {
            TimeOffRequest r = timeOffService.createRequest(u.getId(), req.getType(), req.getStartDate(), req.getEndDate(), req.getPartialDay());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listMine(@RequestParam(name = "mine", defaultValue = "true") boolean mine, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        if (mine) {
            List<TimeOffRequest> list = timeOffService.listForUser(u.getId());
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Integer id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        try {
            TimeOffRequest r = timeOffService.cancelRequest(u.getId(), id);
            return ResponseEntity.ok(r);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", se.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/team")
    public ResponseEntity<?> teamRequests(@RequestParam(name = "status", required = false) String status, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        // only managers
        if (!"MANAGER".equalsIgnoreCase(u.getRole()) && !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<TimeOffRequest> list = timeOffService.listForManager(u.getId(), Optional.ofNullable(status));
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable Integer id, @RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        if (!"MANAGER".equalsIgnoreCase(u.getRole()) && !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String action = body.get("action");
        String comment = body.getOrDefault("comment", null);
        boolean approve = "APPROVE".equalsIgnoreCase(action) || "APPROVED".equalsIgnoreCase(action);
        try {
            TimeOffRequest updated = timeOffService.reviewRequest(u.getId(), id, approve, comment);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
