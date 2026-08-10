package com.example.worktimeoff.config;

import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.UserRepository;
import com.example.worktimeoff.service.TimeOffService;
import com.example.worktimeoff.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final TimeOffService timeOffService;

    public DataInitializer(UserService userService, UserRepository userRepository, TimeOffService timeOffService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.timeOffService = timeOffService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping seed");
            return;
        }

        log.info("Seeding default users (admin, manager, employee)");

        // create admin
        User admin = userService.registerUser("admin@example.com", "AdminPass123!", "Administrator");
        admin.setRole("ADMIN");
        userRepository.save(admin);

        // create manager
        User manager = userService.registerUser("manager@example.com", "ManagerPass123!", "Manager User");
        manager.setRole("MANAGER");
        userRepository.save(manager);

        // create employee and assign manager
        User employee = userService.registerUser("employee@example.com", "EmployeePass123!", "Employee User");
        employee.setManagerId(manager.getId());
        userRepository.save(employee);

        log.info("Seeded users: admin={}, manager={}, employee={}", admin.getEmail(), manager.getEmail(), employee.getEmail());

        // create a sample time-off request for the employee
        try {
            timeOffService.createRequest(employee.getId(), "VACATION", LocalDate.now().plusDays(7), LocalDate.now().plusDays(10), "");
            log.info("Created a sample time-off request for {}", employee.getEmail());
        } catch (Exception ex) {
            log.warn("Could not create sample time-off request: {}", ex.getMessage());
        }

        log.info("Data seeding complete");
    }
}
