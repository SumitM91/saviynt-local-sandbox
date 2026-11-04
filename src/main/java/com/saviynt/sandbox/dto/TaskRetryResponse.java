package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for manual task retry operation.
 */
@Schema(description = "Response returned following an administrative manual task retry")
public record TaskRetryResponse(
        @Schema(description = "Primary task key", example = "31")
        Long taskKey,

        @Schema(description = "Resulting workflow stage", example = "PROVISIONED")
        String workflowStage,

        @Schema(description = "Task status (1 = Completed, 2 = Failed)", example = "1")
        Integer status,

        @Schema(description = "Updated retry count", example = "1")
        int retryCount,

        @Schema(description = "Execution outcome: SUCCESS, RETRYING, or FAILED_TERMINAL", example = "SUCCESS")
        String outcome,

        @Schema(description = "Descriptive outcome message", example = "Task successfully provisioned via manual retry")
        String message
) {
}
