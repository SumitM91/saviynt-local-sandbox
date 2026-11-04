package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Summary DTO for WSRETRY background job batch execution.
 */
@Schema(description = "Batch execution result from WSRETRY background job dispatcher")
public record WsRetryJobResult(
        @Schema(description = "Job name", example = "WSRETRY_DISPATCHER")
        String jobName,

        @Schema(description = "Total number of eligible PROVISIONING_READY tasks evaluated", example = "5")
        int totalScanned,

        @Schema(description = "Tasks successfully provisioned", example = "4")
        int successCount,

        @Schema(description = "Tasks failed or rescheduled for retry", example = "1")
        int failedCount,

        @Schema(description = "High-level execution summary", example = "WSRETRY execution completed: 4 succeeded, 1 scheduled for backoff.")
        String summary,

        @Schema(description = "Detailed list of task processing results")
        List<TaskProvisioningSummary> taskSummaries
) {
}
