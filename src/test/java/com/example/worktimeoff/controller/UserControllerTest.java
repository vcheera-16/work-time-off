package com.example.worktimeoff.controller;

import com.example.worktimeoff.model.User;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User adminUser;
    private User employeeUser;
    private Principal adminPrincipal;
    private Principal empPrincipal;

    @BeforeEach
    void setup() {
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("ADMIN");

        employeeUser = new User();
        employeeUser.setId(2);
        employeeUser.setEmail("emp@example.com");
        employeeUser.setRole("EMPLOYEE");

        adminPrincipal = () -> "admin@example.com";
        empPrincipal = () -> "emp@example.com";
    }

    // --- GET /api/users ---

    @Test
    void getAllUsers_adminRole_returnsUserList() {
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userService.getAllUsers()).thenReturn(List.of(adminUser, employeeUser));

        ResponseEntity<?> response = userController.getAllUsers(adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllUsers_nonAdminRole_returnsForbidden() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employeeUser));

        ResponseEntity<?> response = userController.getAllUsers(empPrincipal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getAllUsers_nullPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = userController.getAllUsers(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    void deleteUser_adminDeletingOtherUser_returnsOk() {
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        doNothing().when(userService).deleteUser(2, 1);

        ResponseEntity<?> response = userController.deleteUser(2, adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("message"));
    }

    @Test
    void deleteUser_nonAdmin_returnsForbidden() {
        when(userService.findByEmail("emp@example.com")).thenReturn(Optional.of(employeeUser));

        ResponseEntity<?> response = userController.deleteUser(1, empPrincipal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(userService, never()).deleteUser(anyInt(), anyInt());
    }

    @Test
    void deleteUser_nullPrincipal_returnsUnauthorized() {
        ResponseEntity<?> response = userController.deleteUser(2, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deleteUser_userNotFound_returnsNotFound() {
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        doThrow(new IllegalArgumentException("User not found"))
                .when(userService).deleteUser(99, 1);

        ResponseEntity<?> response = userController.deleteUser(99, adminPrincipal);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteUser_selfDelete_returnsBadRequest() {
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        doThrow(new IllegalStateException("Cannot delete the currently logged-in user"))
                .when(userService).deleteUser(1, 1);

        ResponseEntity<?> response = userController.deleteUser(1, adminPrincipal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteUser_pendingRequests_returnsBadRequest() {
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        doThrow(new IllegalStateException("Cannot delete user with pending time off requests"))
                .when(userService).deleteUser(2, 1);

        ResponseEntity<?> response = userController.deleteUser(2, adminPrincipal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
