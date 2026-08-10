package com.example.worktimeoff.dto;

import java.time.LocalDate;

public class CreateTimeOffRequest {
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String partialDay;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPartialDay() { return partialDay; }
    public void setPartialDay(String partialDay) { this.partialDay = partialDay; }
}
