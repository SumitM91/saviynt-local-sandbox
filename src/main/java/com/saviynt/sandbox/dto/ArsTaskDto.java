package com.saviynt.sandbox.dto;

public record ArsTaskDto(
        long taskkey,
        int tasktype,
        String tasktypeName,
        String taskdescription,
        Long accountkey,
        String accountname,
        String endpoint,
        String status,
        String comments
) {}
