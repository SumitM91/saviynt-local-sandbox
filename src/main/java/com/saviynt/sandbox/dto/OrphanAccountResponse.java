package com.saviynt.sandbox.dto;

public record OrphanAccountResponse(
        String endpoint,
        Long accountKey,
        String accountName,
        String targetEmpId,
        String accountType
) {}
