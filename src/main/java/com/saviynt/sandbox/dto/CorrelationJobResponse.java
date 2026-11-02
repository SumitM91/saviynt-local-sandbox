package com.saviynt.sandbox.dto;

public record CorrelationJobResponse(
        String jobName,
        int newlyCorrelatedAccounts,
        String message
) {}
