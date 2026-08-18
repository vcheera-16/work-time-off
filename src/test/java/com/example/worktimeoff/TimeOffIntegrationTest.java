package com.example.worktimeoff;

import com.example.worktimeoff.model.TimeOffRequest;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.TimeOffRequestRepository;
import com.example.worktimeoff.repository.UserRepository;
import com.example.worktimeoff.service.TimeOffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimeOffIntegrationTest {

    @Autowired
    private TimeOffService timeOffService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeOffRequestRepository timeOffRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User employee;
    private User manager;

    @BeforeEach
    void setup() {
        manager = new User();
        manager.setEmail("mgr@integration.test");
        manager.setPasswordHash(passwordEncoder.encode("password"));
        manager.setFullName("Integration Manager");
        manager.setRole("MANAGER");
        manager = userRepository.save(manager);

        employee = new User();
        employee.setEmail("emp@integration.test");
        employee.setPasswordHash(passwordEncoder.encode("password"));
        employee.setFullName("Integration Employee");
        employee.setRole("EMPLOYEE");
        employee.setManagerId(manager.getId());
        employee = userRepository.save(employee);
    }

    @Test
    void createRequest_andRetrieve_works() {
        TimeOffRequest r = timeOffService.createRequest(
            employee.getId(), "PTO",
            LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 12), null);

        assertNotNull(r.getId());
        assertEquals("PENDING", r.getStatus());
        assertEquals("PTO", r.getType());
    }

    @Test
    void countUsedPTOsThisYear_excludesWeekendsAndHolidays() {
        // Create an approved request for a full work week in current year
        // In 2026, March 2 (Mon) - March 6 (Fri) has no federal holidays
        int year = LocalDate.now().getYear();
        TimeOffRequest r = new TimeOffRequest();
        r.setUserId(employee.getId());
        r.setType("PTO");
        r.setStartDate(LocalDate.of(year, 3, 2));
        r.setEndDate(LocalDate.of(year, 3, 6));
        r.setStatus("APPROVED");
        timeOffRequestRepository.save(r);

        long count = timeOffService.countUsedPTOsThisYear(employee.getId());
        assertEquals(5, count, "Mon-Fri with no holidays should be 5 business days");
    }

    @Test
    void countUsedPTOsThisYear_excludesChristmasAndWeekend() {
        int year = LocalDate.now().getYear();
        // Find a Dec 24 that is Monday: 2018. But we need current year.
        // For year 2026: Dec 24 = Thu, Dec 25 = Fri (Christmas), Dec 26 = Sat, Dec 27 = Sun, Dec 28 = Mon
        // Business days: Dec 24 (Thu) + Dec 28 (Mon) = 2
        // We'll test via the utility directly
        long days = com.example.worktimeoff.util.BusinessDayCalculator
            .countBusinessDays(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 28));
        assertEquals(2, days);
    }

    @Test
    void createRequest_overlapping_throwsException() {
        timeOffService.createRequest(employee.getId(), "PTO",
            LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 9), null);

        assertThrows(IllegalStateException.class, () ->
            timeOffService.createRequest(employee.getId(), "PTO",
                LocalDate.of(2025, 5, 7), LocalDate.of(2025, 5, 11), null));
    }

    @Test
    void cancelRequest_pending_changesStatusToCancelled() {
        TimeOffRequest r = timeOffService.createRequest(employee.getId(), "PTO",
            LocalDate.of(2025, 6, 2), LocalDate.of(2025, 6, 4), null);

        TimeOffRequest cancelled = timeOffService.cancelRequest(employee.getId(), r.getId());
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void reviewRequest_managerApproves_changesStatusToApproved() {
        TimeOffRequest r = timeOffService.createRequest(employee.getId(), "PTO",
            LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 9), null);

        TimeOffRequest approved = timeOffService.reviewRequest(manager.getId(), r.getId(), true, "Approved!");
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("Approved!", approved.getManagerComment());
    }

    @Test
    void listForUser_returnsOnlyUserRequests() {
        timeOffService.createRequest(employee.getId(), "PTO",
            LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 6), null);

        var list = timeOffService.listForUser(employee.getId());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().allMatch(dto -> dto.getUserId().equals(employee.getId())));
    }

    @Test
    void listForManager_returnsTeamRequests() {
        timeOffService.createRequest(employee.getId(), "PTO",
            LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 3), null);

        var list = timeOffService.listForManager(manager.getId(), Optional.empty());
        assertFalse(list.isEmpty());
    }
}
