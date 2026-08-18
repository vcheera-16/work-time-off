package com.example.worktimeoff.controller;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.service.TimeOffService;
import com.example.worktimeoff.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TimeOffService timeOffService;
    private final UserService userService;

    public DashboardController(TimeOffService timeOffService, UserService userService) {
        this.timeOffService = timeOffService;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();

        Map<String, Object> stats = new HashMap<>();
        
        // Available PTOs (assuming 20 days per year)
        long usedPTOs = timeOffService.countUsedPTOsThisYear(u.getId());
        long availablePTOs = 20 - usedPTOs;
        
        stats.put("username", u.getFullName() != null ? u.getFullName() : u.getEmail());
        stats.put("role", u.getRole());
        stats.put("availablePTOs", Math.max(0, availablePTOs));
        stats.put("usedPTOs", usedPTOs);
        stats.put("totalPTOs", 20);
        stats.put("pendingApprovals", timeOffService.listPendingForUser(u.getId()).size());
        stats.put("upcomingHolidays", getUpcomingFederalHolidays());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User u = userService.findByEmail(principal.getName()).orElseThrow();
        
        return ResponseEntity.ok(timeOffService.listPendingForUser(u.getId()));
    }

    @GetMapping("/holidays")
    public ResponseEntity<?> getHolidays() {
        return ResponseEntity.ok(getUpcomingFederalHolidays());
    }

    private List<Map<String, Object>> getUpcomingFederalHolidays() {
        // Static list of 2024-2026 federal holidays
        return Arrays.asList(
            createHoliday("New Year's Day", "2024-01-01"),
            createHoliday("MLK Jr. Day", "2024-01-15"),
            createHoliday("Presidents Day", "2024-02-19"),
            createHoliday("Memorial Day", "2024-05-27"),
            createHoliday("Independence Day", "2024-07-04"),
            createHoliday("Labor Day", "2024-09-02"),
            createHoliday("Columbus Day", "2024-10-14"),
            createHoliday("Veterans Day", "2024-11-11"),
            createHoliday("Thanksgiving", "2024-11-28"),
            createHoliday("Christmas", "2024-12-25"),
            createHoliday("New Year's Day", "2025-01-01"),
            createHoliday("MLK Jr. Day", "2025-01-20"),
            createHoliday("Presidents Day", "2025-02-17"),
            createHoliday("Memorial Day", "2025-05-26"),
            createHoliday("Independence Day", "2025-07-04"),
            createHoliday("Labor Day", "2025-09-01"),
            createHoliday("Columbus Day", "2025-10-13"),
            createHoliday("Veterans Day", "2025-11-11"),
            createHoliday("Thanksgiving", "2025-11-27"),
            createHoliday("Christmas", "2025-12-25"),
            createHoliday("New Year's Day", "2026-01-01"),
            createHoliday("MLK Jr. Day", "2026-01-19"),
            createHoliday("Presidents Day", "2026-02-16"),
            createHoliday("Memorial Day", "2026-05-25"),
            createHoliday("Independence Day", "2026-07-04"),
            createHoliday("Labor Day", "2026-09-07"),
            createHoliday("Columbus Day", "2026-10-12"),
            createHoliday("Veterans Day", "2026-11-11"),
            createHoliday("Thanksgiving", "2026-11-26"),
            createHoliday("Christmas", "2026-12-25")
        );
    }

    private Map<String, Object> createHoliday(String name, String date) {
        Map<String, Object> holiday = new HashMap<>();
        holiday.put("name", name);
        holiday.put("date", date);
        return holiday;
    }
}
