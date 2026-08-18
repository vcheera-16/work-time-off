package com.example.worktimeoff.controller;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can view all users"));
        }
        
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/managers")
    public ResponseEntity<?> getManagers(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can view managers"));
        }
        
        List<User> managers = userService.getManagers();
        return ResponseEntity.ok(managers);
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User admin = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can create employees"));
        }
        
        try {
            String email = (String) body.get("email");
            String fullName = (String) body.get("fullName");
            Integer managerId = ((Number) body.get("managerId")).intValue();
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Full name is required"));
            }
            if (managerId == null || managerId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Valid manager ID is required"));
            }
            
            User newEmployee = userService.createEmployee(email, fullName, managerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(newEmployee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User admin = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can update users"));
        }
        
        try {
            String fullName = (String) body.get("fullName");
            String role = (String) body.get("role");
            Integer managerId = body.get("managerId") != null ? ((Number) body.get("managerId")).intValue() : null;
            
            User updated = userService.updateUser(id, fullName, role, managerId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
