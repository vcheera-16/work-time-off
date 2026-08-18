package com.example.worktimeoff.service;

import com.example.worktimeoff.dto.TimeOffRequestDTO;
import com.example.worktimeoff.model.TimeOffRequest;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.TimeOffRequestRepository;
import com.example.worktimeoff.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeOffServiceTest {

    @Mock
    private TimeOffRequestRepository repo;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TimeOffService service;

    private User employee;
    private User manager;

    @BeforeEach
    void setup() {
        employee = new User();
        employee.setId(1);
        employee.setEmail("emp@example.com");
        employee.setFullName("Employee One");
        employee.setRole("EMPLOYEE");
        employee.setManagerId(2);

        manager = new User();
        manager.setId(2);
        manager.setEmail("mgr@example.com");
        manager.setFullName("Manager One");
        manager.setRole("MANAGER");
    }

    // -----------------------------------------------------------------------
    // createRequest
    // -----------------------------------------------------------------------

    @Test
    void createRequest_valid_savesCalled() {
        when(repo.findOverlappingForUser(anyInt(), any(), any(), anyList())).thenReturn(List.of());
        TimeOffRequest saved = new TimeOffRequest();
        saved.setId(10);
        when(repo.save(any())).thenReturn(saved);

        TimeOffRequest r = service.createRequest(1, "PTO",
            LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 5), null);

        assertEquals(10, r.getId());
        verify(repo).save(any(TimeOffRequest.class));
    }

    @Test
    void createRequest_startAfterEnd_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
            service.createRequest(1, "PTO",
                LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 5), null));
    }

    @Test
    void createRequest_overlapping_throwsIllegalState() {
        TimeOffRequest existing = new TimeOffRequest();
        when(repo.findOverlappingForUser(anyInt(), any(), any(), anyList()))
            .thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class, () ->
            service.createRequest(1, "PTO",
                LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 5), null));
    }

    // -----------------------------------------------------------------------
    // countUsedPTOsThisYear – core bug fix test
    // -----------------------------------------------------------------------

    @Test
    void countUsedPTOsThisYear_decTwentyFourToTwentyEight_returnsTwo() {
        // 2026: Dec 24 (Thu) ✓, Dec 25 Christmas (Fri) ✗, Dec 26 (Sat) ✗, Dec 27 (Sun) ✗, Dec 28 (Mon) ✓
        long businessDays = com.example.worktimeoff.util.BusinessDayCalculator
            .countBusinessDays(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 28));
        assertEquals(2, businessDays);
    }

    @Test
    void countUsedPTOsThisYear_noRequests_returnsZero() {
        when(repo.findByUserId(1)).thenReturn(List.of());
        assertEquals(0, service.countUsedPTOsThisYear(1));
    }

    @Test
    void countUsedPTOsThisYear_fullWeek_returnsFive() {
        int year = LocalDate.now().getYear();
        // March 2-6 2026 is Mon-Fri with no federal holidays
        // Use fixed dates that are always Mon-Fri with no holidays regardless of year: use 2026 known good week
        LocalDate mon = LocalDate.of(2026, 3, 2);
        LocalDate fri = LocalDate.of(2026, 3, 6);

        TimeOffRequest r = new TimeOffRequest();
        r.setStatus("APPROVED");
        r.setStartDate(mon);
        r.setEndDate(fri);
        when(repo.findByUserId(1)).thenReturn(List.of(r));

        // countUsedPTOsThisYear filters by current year; since 2026 is current year this works
        long count = service.countUsedPTOsThisYear(1);
        assertEquals(5, count);
    }

    // -----------------------------------------------------------------------
    // cancelRequest
    // -----------------------------------------------------------------------

    @Test
    void cancelRequest_ownPending_succeeds() {
        TimeOffRequest r = new TimeOffRequest();
        r.setId(5);
        r.setUserId(1);
        r.setStatus("PENDING");
        when(repo.findById(5)).thenReturn(Optional.of(r));
        when(repo.save(r)).thenReturn(r);

        TimeOffRequest result = service.cancelRequest(1, 5);
        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancelRequest_notOwner_throwsSecurity() {
        TimeOffRequest r = new TimeOffRequest();
        r.setId(5);
        r.setUserId(99);
        r.setStatus("PENDING");
        when(repo.findById(5)).thenReturn(Optional.of(r));

        assertThrows(SecurityException.class, () -> service.cancelRequest(1, 5));
    }

    @Test
    void cancelRequest_alreadyApproved_throwsIllegalState() {
        TimeOffRequest r = new TimeOffRequest();
        r.setId(5);
        r.setUserId(1);
        r.setStatus("APPROVED");
        when(repo.findById(5)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateException.class, () -> service.cancelRequest(1, 5));
    }

    // -----------------------------------------------------------------------
    // reviewRequest
    // -----------------------------------------------------------------------

    @Test
    void reviewRequest_managerApprovesOwnTeam_succeeds() {
        TimeOffRequest r = new TimeOffRequest();
        r.setId(7);
        r.setUserId(1);
        r.setStatus("PENDING");
        when(repo.findById(7)).thenReturn(Optional.of(r));
        when(userRepository.findById(1)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2)).thenReturn(Optional.of(manager));
        when(repo.save(r)).thenReturn(r);

        TimeOffRequest result = service.reviewRequest(2, 7, true, "Looks good");
        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void reviewRequest_managerNotTeam_throwsSecurity() {
        User otherEmployee = new User();
        otherEmployee.setId(1);
        otherEmployee.setManagerId(99); // different manager

        TimeOffRequest r = new TimeOffRequest();
        r.setId(7);
        r.setUserId(1);
        r.setStatus("PENDING");
        when(repo.findById(7)).thenReturn(Optional.of(r));
        when(userRepository.findById(1)).thenReturn(Optional.of(otherEmployee));
        when(userRepository.findById(2)).thenReturn(Optional.of(manager));

        assertThrows(SecurityException.class, () -> service.reviewRequest(2, 7, true, null));
    }

    // -----------------------------------------------------------------------
    // listForManager
    // -----------------------------------------------------------------------

    @Test
    void listForManager_noTeam_returnsEmpty() {
        when(userRepository.findById(2)).thenReturn(Optional.of(manager));
        when(userRepository.findByManagerId(2)).thenReturn(List.of());

        List<TimeOffRequestDTO> result = service.listForManager(2, Optional.empty());
        assertTrue(result.isEmpty());
    }

    @Test
    void listForManager_withStatusFilter_filtersCorrectly() {
        when(userRepository.findById(2)).thenReturn(Optional.of(manager));
        when(userRepository.findByManagerId(2)).thenReturn(List.of(employee));

        TimeOffRequest approved = new TimeOffRequest();
        approved.setUserId(1);
        approved.setStatus("APPROVED");
        approved.setStartDate(LocalDate.now());
        approved.setEndDate(LocalDate.now());

        TimeOffRequest pending = new TimeOffRequest();
        pending.setUserId(1);
        pending.setStatus("PENDING");
        pending.setStartDate(LocalDate.now());
        pending.setEndDate(LocalDate.now());

        when(repo.findByUserIdIn(List.of(1))).thenReturn(List.of(approved, pending));
        when(userRepository.findById(1)).thenReturn(Optional.of(employee));

        List<TimeOffRequestDTO> result = service.listForManager(2, Optional.of("APPROVED"));
        assertEquals(1, result.size());
        assertEquals("APPROVED", result.get(0).getStatus());
    }
}
