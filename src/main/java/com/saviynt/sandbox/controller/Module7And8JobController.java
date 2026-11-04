package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.FailedTaskResponse;
import com.saviynt.sandbox.dto.TaskRetryResponse;
import com.saviynt.sandbox.dto.WsRetryJobResult;
import com.saviynt.sandbox.service.Module7And8ProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Enterprise REST Controller for Module 7 & 8: Job Control Panel, WSRETRY Engine & Target Connectors.
 */
@RestController
@RequestMapping("/saviynt/module7-8")
@Tag(name = "Module 7 & 8: Job Control Panel (WSRETRY) & Target Connectors",
     description = "Saviynt WSRETRY Outbound Dispatcher Engine, Exponential Backoff, and Target Provisioning Connectors (REST & Active Directory)")
public class Module7And8JobController {

    private final Module7And8ProvisioningService provisioningService;

    public Module7And8JobController(Module7And8ProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    // ==========================================
    // 1. TRIGGER WSRETRY BACKGROUND DISPATCHER
    // ==========================================
    @PostMapping("/jobs/wsretry/trigger")
    @Operation(
            summary = "Trigger WSRETRY Background Dispatcher Job",
            description = "Scans all tasks where workflow_stage = 'PROVISIONING_READY' and status = 1 and retryCount < 3. " +
                    "Dispatches provisioning through ConnectorFactory with exponential backoff on failure."
    )
    @ApiResponse(responseCode = "200", description = "WSRETRY batch execution processed successfully",
            content = @Content(schema = @Schema(implementation = WsRetryJobResult.class)))
    public ResponseEntity<WsRetryJobResult> triggerWsRetryJob() {
        return ResponseEntity.ok(provisioningService.dispatchReadyTasks());
    }

    // ==========================================
    // 2. ADMINISTRATIVE MANUAL RETRY BY TASK KEY
    // ==========================================
    @PostMapping("/tasks/{taskKey}/retry")
    @Operation(
            summary = "Manually Retry Failed or Ready Provisioning Task",
            description = "Triggers an on-demand administrative retry for a specific ARS task from the Job Control Panel."
    )
    @ApiResponse(responseCode = "200", description = "Manual retry executed successfully",
            content = @Content(schema = @Schema(implementation = TaskRetryResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task not found with specified key")
    public ResponseEntity<TaskRetryResponse> manualRetryTask(
            @Parameter(description = "Primary task key", example = "31", required = true)
            @PathVariable("taskKey") Long taskKey
    ) {
        return ResponseEntity.ok(provisioningService.manualRetryTask(taskKey));
    }

    // ==========================================
    // 3. RETRIEVE FAILED / ACTION-REQUIRED TASKS
    // ==========================================
    @GetMapping("/tasks/failed")
    @Operation(
            summary = "List Failed / Action-Required Provisioning Tasks",
            description = "Retrieves all ARS tasks that have exhausted maximum retries (status = 2 or retryCount >= 3) for admin review in Job Control Panel."
    )
    @ApiResponse(responseCode = "200", description = "List of failed tasks returned successfully")
    public ResponseEntity<List<FailedTaskResponse>> getFailedTasks() {
        return ResponseEntity.ok(provisioningService.getFailedTasks());
    }
}
