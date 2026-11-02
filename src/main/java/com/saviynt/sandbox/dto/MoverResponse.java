package com.saviynt.sandbox.dto;

public record MoverResponse(
        String lifecycleEvent,
        long userkey,
        String username,
        String previousDepartment,
        String newDepartment,
        String previousManager,
        String newManager,
        String moverEvaluation,
        String message
) {}
