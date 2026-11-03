package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.WorkflowApprovalRequest;
import com.saviynt.sandbox.dto.WorkflowResponse;
import com.saviynt.sandbox.dto.WorkflowSubmitRequest;
import com.saviynt.sandbox.service.WorkflowService;
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
 * Enterprise REST Controller for Module 5 & 6: ARS Request Cart & Multi-Level Workflows.
 *
 * Adheres strictly to Clean 3-Tier Architecture with zero SQL queries.
 * Database persistence is isolated in ArsTaskRepository (Spring Data JPA), and business logic / state machine
 * transitions are handled entirely in WorkflowService.
 */
@RestController
@RequestMapping("/saviynt/module5-6")
@Tag(name = "Module 5 & 6: ARS Request Cart & Multi-Level Workflows",
     description = "Saviynt ARS Request Cart Access Requests & Multi-Level Approval State Machine Engine")
public class Module5And6WorkflowController {

    private final WorkflowService workflowService;

    public Module5And6WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // ==========================================
    // 1. SUBMIT ARS REQUEST CART ACCESS REQUEST
    // ==========================================
    @PostMapping("/submit-request")
    @Operation(
            summary = "Submit ARS Request Cart Access Request",
            description = "Accepts request parameters (userKey, requestedItem, justification) and initializes an ARS task " +
                    "with stage MANAGER_PENDING in the arstasks table."
    )
    @ApiResponse(responseCode = "200", description = "Access request submitted and initialized in MANAGER_PENDING stage",
            content = @Content(schema = @Schema(implementation = WorkflowResponse.class)))
    public ResponseEntity<WorkflowResponse> submitRequest(
            @RequestBody(required = false) WorkflowSubmitRequest requestBody,
            @Parameter(description = "User Key requesting access (query param fallback)", example = "101")
            @RequestParam(name = "userKey", required = false) Long userKey,
            @Parameter(description = "Requested entitlement or role (query param fallback)", example = "AWS_Production_Admin")
            @RequestParam(name = "requestedItem", required = false) String requestedItem,
            @Parameter(description = "Business justification (query param fallback)", example = "Production deployment and troubleshooting")
            @RequestParam(name = "justification", required = false) String justification
    ) {
        Long targetUserKey = (requestBody != null && requestBody.userKey() != null) ? requestBody.userKey() : userKey;
        String targetItem = (requestBody != null && requestBody.requestedItem() != null) ? requestBody.requestedItem() : requestedItem;
        String targetJustification = (requestBody != null && requestBody.justification() != null) ? requestBody.justification() : justification;

        WorkflowSubmitRequest effectiveRequest = new WorkflowSubmitRequest(targetUserKey, targetItem, targetJustification);
        return ResponseEntity.ok(workflowService.submitRequest(effectiveRequest));
    }

    // ==========================================
    // 2. PROCESS MULTI-LEVEL WORKFLOW APPROVAL
    // ==========================================
    @PostMapping("/process-approval/{taskKey}")
    @Operation(
            summary = "Process Multi-Level Workflow Approval Decision",
            description = "Advances stage based on action (APPROVE/REJECT) and approverRole (MANAGER/APP_OWNER): " +
                    "MANAGER approval moves stage from MANAGER_PENDING to APP_OWNER_PENDING. " +
                    "APP_OWNER approval moves stage from APP_OWNER_PENDING to PROVISIONING_READY with status 1. " +
                    "REJECT moves stage to REJECTED with status -1."
    )
    @ApiResponse(responseCode = "200", description = "Approval action processed and stage advanced successfully",
            content = @Content(schema = @Schema(implementation = WorkflowResponse.class)))
    public ResponseEntity<WorkflowResponse> processApproval(
            @Parameter(description = "Primary task key of the ARS task", example = "1", required = true)
            @PathVariable("taskKey") long taskKey,
            @RequestBody(required = false) WorkflowApprovalRequest requestBody,
            @Parameter(description = "Action: APPROVE or REJECT (query param fallback)", example = "APPROVE")
            @RequestParam(name = "action", required = false) String action,
            @Parameter(description = "Approver Role: MANAGER or APP_OWNER (query param fallback)", example = "MANAGER")
            @RequestParam(name = "approverRole", required = false) String approverRole,
            @Parameter(description = "Comments or remarks (query param fallback)", example = "Approved after review")
            @RequestParam(name = "comments", required = false) String comments
    ) {
        String targetAction = (requestBody != null && requestBody.action() != null) ? requestBody.action() : action;
        String targetRole = (requestBody != null && requestBody.approverRole() != null) ? requestBody.approverRole() : approverRole;
        String targetComments = (requestBody != null && requestBody.comments() != null) ? requestBody.comments() : comments;

        WorkflowApprovalRequest effectiveRequest = new WorkflowApprovalRequest(targetAction, targetRole, targetComments);
        return ResponseEntity.ok(workflowService.processApproval(taskKey, effectiveRequest));
    }

    // ==========================================
    // 3. RETRIEVE TASK BY KEY
    // ==========================================
    @GetMapping("/tasks/{taskKey}")
    @Operation(
            summary = "Get ARS Workflow Task by Key",
            description = "Retrieves an ARS workflow task details and approval audit history by taskKey."
    )
    @ApiResponse(responseCode = "200", description = "Task details returned successfully")
    public ResponseEntity<WorkflowResponse> getTaskById(
            @Parameter(description = "Primary task key", example = "1", required = true)
            @PathVariable("taskKey") long taskKey
    ) {
        return ResponseEntity.ok(workflowService.getTaskById(taskKey));
    }

    // ==========================================
    // 4. RETRIEVE ALL WORKFLOW TASKS
    // ==========================================
    @GetMapping("/tasks")
    @Operation(
            summary = "Get All ARS Workflow Tasks",
            description = "Retrieves all ARS workflow tasks with current stage and status."
    )
    @ApiResponse(responseCode = "200", description = "List of workflow tasks")
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflowTasks() {
        return ResponseEntity.ok(workflowService.getAllWorkflowTasks());
    }
}
