package com.example.worktimeoff.service;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.TimeOffRequestRepository;
import com.example.worktimeoff.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeOffRequestRepository timeOffRequestRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TimeOffRequestRepository timeOffRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.timeOffRequestRepository = timeOffRequestRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getManagers() {
        return userRepository.findAll().stream()
            .filter(u -> "MANAGER".equals(u.getRole()))
            .collect(Collectors.toList());
    }

    public User registerUser(String email, String rawPassword, String fullName) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setRole("EMPLOYEE");
        return userRepository.save(u);
    }

    public User createUser(String email, String fullName, String role, Integer managerId) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(passwordEncoder.encode("TempPassword123!"));
        u.setFullName(fullName);
        u.setRole(role != null ? role.toUpperCase() : "EMPLOYEE");
        u.setManagerId(managerId);
        return userRepository.save(u);
    }

    public User createEmployee(String email, String fullName, Integer managerId) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(passwordEncoder.encode("TempPassword123!")); // Temp password
        u.setFullName(fullName);
        u.setRole("EMPLOYEE");
        u.setManagerId(managerId);
        return userRepository.save(u);
    }

    public User updateUser(Integer id, String fullName, String role, Integer managerId) {
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (fullName != null) u.setFullName(fullName);
        if (role != null) u.setRole(role);
        if (managerId != null) u.setManagerId(managerId);
        return userRepository.save(u);
    }

    public void deleteUser(Integer id, Integer currentUserId) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (id.equals(currentUserId)) {
            throw new IllegalStateException("Cannot delete the currently logged-in user");
        }
        List<com.example.worktimeoff.model.TimeOffRequest> pending =
                timeOffRequestRepository.findByUserId(id).stream()
                        .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
        if (!pending.isEmpty()) {
            throw new IllegalStateException("Cannot delete user with pending time off requests");
        }
        logger.info("Deleting user id={} email={}", u.getId(), u.getEmail());
        timeOffRequestRepository.deleteAll(timeOffRequestRepository.findByUserId(id));
        userRepository.delete(u);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username.toLowerCase()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPasswordHash(), authorities);
    }
}
