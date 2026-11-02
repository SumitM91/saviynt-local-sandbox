package com.saviynt.sandbox.dto;

import java.util.List;

public record LeaverResponse(
        String lifecycleEvent,
        long userkey,
        String username,
        int statuskey,
        String statusLabel,
        int linkedAccountsCount,
        List<ArsTaskDto> deprovisioningTasks,
        String message
) {}
