package com.example.worktimeoff.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class TimeOffRequestDTO {
    private Integer id;
    private Integer userId;
    private String userName; // Employee name who requested
    private String userEmail;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String partialDay;
    private String status;
    private OffsetDateTime requestedAt;
    private Integer reviewedBy;
    private String reviewedByName; // Manager/Admin who approved/denied
    private String reviewedByEmail;
    private OffsetDateTime reviewedAt;
    private String managerComment;

    public TimeOffRequestDTO() {}

    public TimeOffRequestDTO(Integer id, Integer userId, String userName, String userEmail, String type, 
                            LocalDate startDate, LocalDate endDate, String partialDay, String status,
                            OffsetDateTime requestedAt, Integer reviewedBy, String reviewedByName, 
                            String reviewedByEmail, OffsetDateTime reviewedAt, String managerComment) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.partialDay = partialDay;
        this.status = status;
        this.requestedAt = requestedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedByName = reviewedByName;
        this.reviewedByEmail = reviewedByEmail;
        this.reviewedAt = reviewedAt;
        this.managerComment = managerComment;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPartialDay() { return partialDay; }
    public void setPartialDay(String partialDay) { this.partialDay = partialDay; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getReviewedByEmail() { return reviewedByEmail; }
    public void setReviewedByEmail(String reviewedByEmail) { this.reviewedByEmail = reviewedByEmail; }

    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
}
