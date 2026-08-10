package com.example.worktimeoff.service;

import com.example.worktimeoff.model.TimeOffRequest;
import com.example.worktimeoff.model.User;
import com.example.worktimeoff.repository.TimeOffRequestRepository;
import com.example.worktimeoff.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimeOffService {

    private final TimeOffRequestRepository repo;
    private final UserRepository userRepository;

    public TimeOffService(TimeOffRequestRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public TimeOffRequest createRequest(Integer userId, String type, LocalDate start, LocalDate end, String partialDay) {
        if (start.isAfter(end)) throw new IllegalArgumentException("start date must be before or equal to end date");
        // prevent overlapping PENDING or APPROVED
        List<String> statuses = List.of("PENDING","APPROVED");
        List<TimeOffRequest> overlaps = repo.findOverlappingForUser(userId, start, end, statuses);
        if (!overlaps.isEmpty()) throw new IllegalStateException("You have overlapping time off requests");

        TimeOffRequest r = new TimeOffRequest();
        r.setUserId(userId);
        r.setType(type);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setPartialDay(partialDay);
        r.setStatus("PENDING");
        return repo.save(r);
    }

    public List<TimeOffRequest> listForUser(Integer userId) {
        return repo.findByUserId(userId);
    }

    public List<TimeOffRequest> listForManager(Integer managerId, Optional<String> statusFilter) {
        List<User> team = userRepository.findByManagerId(managerId);
        List<Integer> ids = team.stream().map(User::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return List.of();
        List<TimeOffRequest> requests = repo.findByUserIdIn(ids);
        if (statusFilter.isPresent()) {
            String s = statusFilter.get();
            return requests.stream().filter(r -> s.equalsIgnoreCase(r.getStatus())).collect(Collectors.toList());
        }
        return requests;
    }

    public Optional<TimeOffRequest> findById(Integer id) {
        return repo.findById(id);
    }

    public TimeOffRequest cancelRequest(Integer userId, Integer requestId) {
        TimeOffRequest r = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (!r.getUserId().equals(userId)) throw new SecurityException("not owner");
        if (!"PENDING".equals(r.getStatus())) throw new IllegalStateException("only pending requests can be cancelled");
        r.setStatus("CANCELLED");
        return repo.save(r);
    }

    public TimeOffRequest reviewRequest(Integer managerId, Integer requestId, boolean approve, String comment) {
        TimeOffRequest r = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("not found"));
        User targetUser = userRepository.findById(r.getUserId()).orElseThrow();
        if (targetUser.getManagerId() == null || !targetUser.getManagerId().equals(managerId)) {
            throw new SecurityException("not authorized to review");
        }
        r.setReviewedBy(managerId);
        r.setManagerComment(comment);
        r.setReviewedAt(java.time.OffsetDateTime.now());
        r.setStatus(approve ? "APPROVED" : "DENIED");
        return repo.save(r);
    }
}
