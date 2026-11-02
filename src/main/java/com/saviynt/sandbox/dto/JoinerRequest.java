package com.saviynt.sandbox.dto;

public record JoinerRequest(
        String username,
        String firstname,
        String lastname,
        String email,
        String departmentname,
        String employeetype,
        String manager,
        Long orgkey,
        String employeeId
) {}
