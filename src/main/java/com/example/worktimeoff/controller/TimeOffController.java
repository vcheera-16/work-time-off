package com.example.worktimeoff.controller;

import com.example.worktimeoff.dto.TimeOffRequestDTO;
import com.example.worktimeoff.dto.CreateTimeOffRequest;
import com.example.worktimeoff.model.TimeOffRequest;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.service.TimeOffService;
import com.example.worktimeoff.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public ResponseEntity<?> create(@Valid @RequestBody CreateTimeOffRequest req, BindingResult binding, Principal principal) {
        if (binding.hasErrors()) {
            return ResponseEntity.badRequest().body(binding.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList()));
        }
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
    public ResponseEntity<?> listMine(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        List<TimeOffRequestDTO> list = timeOffService.listForUser(u.getId());
        return ResponseEntity.ok(list);
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
        
        // Employees cannot see team requests
        if ("EMPLOYEE".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Employees cannot view team requests"));
        }
        
        // Only managers and admins
        if (!"MANAGER".equalsIgnoreCase(u.getRole()) && !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<TimeOffRequestDTO> list;
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            // Admin sees all requests
            list = timeOffService.listAllRequests(Optional.ofNullable(status));
        } else {
            // Manager sees only team requests
            list = timeOffService.listForManager(u.getId(), Optional.ofNullable(status));
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pendingApprovals(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        List<TimeOffRequestDTO> pending = timeOffService.listPendingForUser(u.getId());
        return ResponseEntity.ok(pending);
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
