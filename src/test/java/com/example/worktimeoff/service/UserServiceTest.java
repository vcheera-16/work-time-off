package com.example.worktimeoff.service;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setup() {
        sampleUser = new User();
        sampleUser.setId(1);
        sampleUser.setEmail("test@example.com");
        sampleUser.setPasswordHash("hashed");
        sampleUser.setFullName("Test User");
        sampleUser.setRole("EMPLOYEE");
    }

    @Test
    void findByEmail_exists_returnsUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        Optional<User> result = userService.findByEmail("test@example.com");
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_uppercase_normalizesToLower() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        Optional<User> result = userService.findByEmail("TEST@EXAMPLE.COM");
        assertTrue(result.isPresent());
    }

    @Test
    void findById_exists_returnsUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        assertTrue(userService.findById(1).isPresent());
    }

    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        assertEquals(1, userService.getAllUsers().size());
    }

    @Test
    void getManagers_filtersManagersOnly() {
        User manager = new User();
        manager.setRole("MANAGER");
        when(userRepository.findAll()).thenReturn(List.of(sampleUser, manager));
        List<User> managers = userService.getManagers();
        assertEquals(1, managers.size());
        assertEquals("MANAGER", managers.get(0).getRole());
    }

    @Test
    void registerUser_savesUser() {
        when(passwordEncoder.encode("rawPass")).thenReturn("hashedPass");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser("New@Example.Com", "rawPass", "New User");
        assertEquals("new@example.com", result.getEmail());
        assertEquals("EMPLOYEE", result.getRole());
        assertEquals("hashedPass", result.getPasswordHash());
    }

    @Test
    void createUser_managerRole_setsRoleCorrectly() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("mgr@example.com", "New Mgr", "MANAGER", null);
        assertEquals("MANAGER", result.getRole());
    }

    @Test
    void createUser_nullRole_defaultsToEmployee() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("emp@example.com", "Emp", null, 1);
        assertEquals("EMPLOYEE", result.getRole());
    }

    @Test
    void updateUser_updatesFields() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1, "Updated Name", "MANAGER", 5);
        assertEquals("Updated Name", result.getFullName());
        assertEquals("MANAGER", result.getRole());
        assertEquals(5, result.getManagerId());
    }

    @Test
    void updateUser_notFound_throwsIllegalArgument() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(99, "Name", null, null));
    }

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        UserDetails details = userService.loadUserByUsername("test@example.com");
        assertEquals("test@example.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
            () -> userService.loadUserByUsername("missing@example.com"));
    }
}
