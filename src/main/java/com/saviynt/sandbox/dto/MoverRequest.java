package com.saviynt.sandbox.dto;

public record MoverRequest(
        String username,
        String newDepartment,
        String newManager
) {}
