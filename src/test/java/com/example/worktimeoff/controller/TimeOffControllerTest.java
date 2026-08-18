package com.example.worktimeoff.controller;

import com.example.worktimeoff.dto.CreateTimeOffRequest;
import com.example.worktimeoff.dto.TimeOffRequestDTO;
import com.example.worktimeoff.model.TimeOffRequest;
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
import org.springframework.validation.BindingResult;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeOffControllerTest {

    @Mock
    private TimeOffService timeOffService;

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private TimeOffController timeOffController;

    private User employee;
    private User manager;
    private Principal empPrincipal;
    private Principal mgrPrincipal;

    @BeforeEach
    void setup() {
        employee = new User();
        employee.setId(1);
        employee.setEmail("emp@example.com");
        employee.setRole("EMPLOYEE");
        employee.setManagerId(2);

        manager = new User();
        manager.setId(2);
        manager.setEmail("mgr@example.com");
        manager.setRole("MANAGER");

        empPrincipal = () -> "emp@example.com";
        mgrPrincipal = () -> "mgr@example.com";
    }

    @Test
    void create_validRequest_returnsOk() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));

        TimeOffRequest saved = new TimeOffRequest();
        saved.setId(1);
        when(timeOffService.createRequest(anyInt(), anyString(), any(), any(), any()))
            .thenReturn(saved);

        CreateTimeOffRequest req = new CreateTimeOffRequest();
        req.setType("PTO");
        req.setStartDate(LocalDate.of(2024, 3, 4));
        req.setEndDate(LocalDate.of(2024, 3, 5));

        ResponseEntity<?> response = timeOffController.create(req, bindingResult, empPrincipal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void create_bindingErrors_returnsBadRequest() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        CreateTimeOffRequest req = new CreateTimeOffRequest();
        ResponseEntity<?> response = timeOffController.create(req, bindingResult, empPrincipal);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void create_noPrincipal_returnsUnauthorized() {
        when(bindingResult.hasErrors()).thenReturn(false);
        ResponseEntity<?> response = timeOffController.create(new CreateTimeOffRequest(), bindingResult, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void create_overlap_returnsConflict() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));
        when(timeOffService.createRequest(anyInt(), anyString(), any(), any(), any()))
            .thenThrow(new IllegalStateException("overlap"));

        CreateTimeOffRequest req = new CreateTimeOffRequest();
        req.setType("PTO");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now());

        ResponseEntity<?> response = timeOffController.create(req, bindingResult, empPrincipal);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void listMine_noPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = timeOffController.listMine(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void listMine_authenticated_returnsList() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));
        when(timeOffService.listForUser(1)).thenReturn(List.of());

        ResponseEntity<?> response = timeOffController.listMine(empPrincipal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void teamRequests_employee_returnsForbidden() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));

        ResponseEntity<?> response = timeOffController.teamRequests(null, empPrincipal);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void teamRequests_manager_returnsList() {
        when(userService.findByEmail("mgr@example.com")).thenReturn(Optional.of(manager));
        when(timeOffService.listForManager(eq(2), any())).thenReturn(List.of());

        ResponseEntity<?> response = timeOffController.teamRequests(null, mgrPrincipal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void cancel_notOwner_returnsForbidden() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));
        when(timeOffService.cancelRequest(1, 99)).thenThrow(new SecurityException("not owner"));

        ResponseEntity<?> response = timeOffController.cancel(99, empPrincipal);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void review_employee_returnsForbidden() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employee));

        ResponseEntity<?> response = timeOffController.review(1, Map.of("action", "APPROVE"), empPrincipal);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void review_manager_approvesSuccessfully() {
        when(userService.findByEmail("mgr@example.com")).thenReturn(Optional.of(manager));
        TimeOffRequest updated = new TimeOffRequest();
        updated.setStatus("APPROVED");
        when(timeOffService.reviewRequest(2, 1, true, null)).thenReturn(updated);

        ResponseEntity<?> response = timeOffController.review(1, Map.of("action", "APPROVE"), mgrPrincipal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void review_noPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = timeOffController.review(1, Map.of("action", "APPROVE"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
