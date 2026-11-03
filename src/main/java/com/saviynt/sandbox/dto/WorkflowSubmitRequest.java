package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for ARS Request Cart submission")
public record WorkflowSubmitRequest(
        @Schema(description = "User Key requesting access", example = "101")
        Long userKey,

        @Schema(description = "Requested role, entitlement, or resource", example = "AWS_Production_Admin")
        String requestedItem,

        @Schema(description = "Business justification for access request", example = "Production deployment and troubleshooting")
        String justification
) {}
