package com.saviynt.sandbox.dto;

import java.util.List;

public record JoinerResponse(
        String lifecycleEvent,
        long userkey,
        String username,
        String firstname,
        String lastname,
        String email,
        String departmentname,
        String employeetype,
        String employeeId,
        int statuskey,
        List<ArsTaskDto> provisioningTasks,
        String message
) {}
