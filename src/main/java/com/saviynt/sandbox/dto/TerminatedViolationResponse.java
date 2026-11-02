package com.saviynt.sandbox.dto;

public record TerminatedViolationResponse(
        String terminatedUser,
        Integer userStatusKey,
        String activeTargetAccount,
        String endpoint,
        Integer accountStatus
) {}
