package com.example.worktimeoff.repository;

import com.example.worktimeoff.model.TimeOffRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimeOffRequestRepository extends JpaRepository<TimeOffRequest, Integer> {
    List<TimeOffRequest> findByUserId(Integer userId);
    List<TimeOffRequest> findByUserIdIn(List<Integer> userIds);

    @Query("SELECT r FROM TimeOffRequest r WHERE r.userId = :userId AND r.status IN :statuses AND NOT (r.endDate < :startDate OR r.startDate > :endDate)")
    List<TimeOffRequest> findOverlappingForUser(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("statuses") List<String> statuses);
}
