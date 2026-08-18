package com.example.worktimeoff.controller;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.service.TimeOffService;
import com.example.worktimeoff.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TimeOffService timeOffService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DashboardController dashboardController;

    private User testUser;
    private Principal principal;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("emp@example.com");
        testUser.setFullName("Test User");
        testUser.setRole("EMPLOYEE");

        principal = () -> "emp@example.com";
    }

    @Test
    void getDashboardStats_authenticatedUser_returnsStats() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(testUser));
        when(timeOffService.countUsedPTOsThisYear(1)).thenReturn(3L);
        when(timeOffService.listPendingForUser(1)).thenReturn(List.of());

        ResponseEntity<?> response = dashboardController.getDashboardStats(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(3L, body.get("usedPTOs"));
        assertEquals(17L, body.get("availablePTOs"));
        assertEquals(20, body.get("totalPTOs"));
    }

    @Test
    void getDashboardStats_noPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = dashboardController.getDashboardStats(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getDashboardStats_usedExceedsTotal_availableIsZero() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(testUser));
        when(timeOffService.countUsedPTOsThisYear(1)).thenReturn(25L);
        when(timeOffService.listPendingForUser(1)).thenReturn(List.of());

        ResponseEntity<?> response = dashboardController.getDashboardStats(principal);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(0L, body.get("availablePTOs"));
    }

    @Test
    void getHolidays_returnsNonEmptyList() {
        ResponseEntity<?> response = dashboardController.getHolidays();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<?> holidays = (List<?>) response.getBody();
        assertNotNull(holidays);
        assertFalse(holidays.isEmpty());
    }

    @Test
    void getPendingApprovals_noPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = dashboardController.getPendingApprovals(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getPendingApprovals_authenticated_returnsList() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(testUser));
        when(timeOffService.listPendingForUser(1)).thenReturn(List.of());

        ResponseEntity<?> response = dashboardController.getPendingApprovals(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
