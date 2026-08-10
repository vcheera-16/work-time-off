package com.work.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TimeOffController {
    // Controller methods for handling time off requests will go here

    @GetMapping("/request-time-off")
    public String requestTimeOff() {
        // Logic for requesting time off
        return "Time off requested successfully.";
    }

    @GetMapping("/approve-time-off")
    public String approveTimeOff() {
        // Logic for approving time off
        return "Time off approved successfully.";
    }

    @GetMapping("/deny-time-off")
    public String denyTimeOff() {
        // Logic for denying time off
        return "Time off denied.";
    }

    @GetMapping("/view-time-off-requests")
    public String viewTimeOffRequests() {
        // Logic for viewing time off requests
        return "Displaying all time off requests.";
    }
}