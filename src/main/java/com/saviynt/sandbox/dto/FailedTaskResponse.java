package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Representation of failed or action-required ARS tasks for Job Control Panel review.
 */
@Schema(description = "Details of failed ARS tasks requiring administrative intervention")
public record FailedTaskResponse(
        @Schema(description = "Primary task key", example = "31")
        Long taskKey,

        @Schema(description = "Task type code", example = "1")
        Integer taskType,

        @Schema(description = "Task description", example = "Access Request: AWS_SecurityAdmin_Role")
        String taskDescription,

        @Schema(description = "Associated user key", example = "101")
        Long userKey,

        @Schema(description = "Associated account key", example = "501")
        Long accountKey,

        @Schema(description = "Associated endpoint key", example = "10")
        Long endpointKey,

        @Schema(description = "Task status (2 = Action Required / Failed)", example = "2")
        Integer status,

        @Schema(description = "Workflow stage", example = "PROVISIONING_FAILED")
        String workflowStage,

        @Schema(description = "Total retry attempts recorded", example = "3")
        int retryCount,

        @Schema(description = "Audit comments and error diagnostics", example = "Max retries (3) exhausted. Connection timed out.")
        String comments,

        @Schema(description = "Timestamp when task was created")
        LocalDateTime createDate
) {
}
