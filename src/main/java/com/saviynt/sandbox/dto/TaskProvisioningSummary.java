package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary DTO for an individual ARS task processed during WSRETRY execution.
 */
@Schema(description = "Summary of task provisioning attempt under WSRETRY")
public record TaskProvisioningSummary(
        @Schema(description = "Primary task key", example = "31")
        Long taskKey,

        @Schema(description = "Task description", example = "Access Request: AWS_SecurityAdmin_Role")
        String taskDescription,

        @Schema(description = "Current workflow stage", example = "PROVISIONED")
        String workflowStage,

        @Schema(description = "Task status (1 = Completed, 2 = Action Required / Failed)", example = "1")
        Integer status,

        @Schema(description = "Retry count attempts", example = "0")
        int retryCount,

        @Schema(description = "Execution outcome: SUCCESS, RETRYING, or FAILED_TERMINAL", example = "SUCCESS")
        String outcome,

        @Schema(description = "Audit or error message", example = "Successfully provisioned to target system")
        String message
) {
}
