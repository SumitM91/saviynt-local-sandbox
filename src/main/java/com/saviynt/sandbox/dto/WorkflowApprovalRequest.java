package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for processing multi-level workflow approval decision")
public record WorkflowApprovalRequest(
        @Schema(description = "Approval action: APPROVE or REJECT", example = "APPROVE")
        String action,

        @Schema(description = "Approver role executing the decision: MANAGER or APP_OWNER", example = "MANAGER")
        String approverRole,

        @Schema(description = "Comments or remarks regarding approval/rejection decision", example = "Approved after reviewing team allocation")
        String comments
) {}
