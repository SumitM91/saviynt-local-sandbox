package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response representation for ARS workflow task")
public record WorkflowResponse(
        @Schema(description = "ARS Task Key", example = "1")
        long taskKey,

        @Schema(description = "Task Type ID (1 = Add Access)", example = "1")
        int taskType,

        @Schema(description = "Task description", example = "Access Request: AWS_Production_Admin")
        String taskDescription,

        @Schema(description = "User Key", example = "101")
        Long userKey,

        @Schema(description = "Current task status: 0 = Pending, 1 = Provisioning Ready, -1 = Rejected", example = "0")
        int status,

        @Schema(description = "Current workflow stage: MANAGER_PENDING, APP_OWNER_PENDING, PROVISIONING_READY, REJECTED", example = "MANAGER_PENDING")
        String workflowStage,

        @Schema(description = "Original request comments / justification", example = "Production deployment and troubleshooting")
        String comments,

        @Schema(description = "Audit trail of approver comments", example = "[MANAGER - APPROVE]: Approved after review")
        String approverComments,

        @Schema(description = "Status / outcome summary message", example = "Access request submitted successfully. ARS task initialized in MANAGER_PENDING stage.")
        String message
) {}
