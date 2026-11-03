package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.WorkflowApprovalRequest;
import com.saviynt.sandbox.dto.WorkflowResponse;
import com.saviynt.sandbox.dto.WorkflowSubmitRequest;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Enterprise Service for Module 5 & 6: ARS Request Cart & Multi-Level Workflows.
 *
 * Implements Saviynt EIC Hybrid Backend Architecture with Spring Data JPA and Declarative Transactions:
 * Sequential stage transition invariants:
 * - MANAGER_PENDING -> (MANAGER APPROVE) -> APP_OWNER_PENDING (status 0)
 * - APP_OWNER_PENDING -> (APP_OWNER APPROVE) -> PROVISIONING_READY (status 1)
 * - Non-terminal stage -> (REJECT) -> REJECTED (status -1)
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    public static final String STAGE_MANAGER_PENDING = "MANAGER_PENDING";
    public static final String STAGE_APP_OWNER_PENDING = "APP_OWNER_PENDING";
    public static final String STAGE_PROVISIONING_READY = "PROVISIONING_READY";
    public static final String STAGE_REJECTED = "REJECTED";

    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_APP_OWNER = "APP_OWNER";

    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROVISIONING_READY = 1;
    public static final int STATUS_REJECTED = -1;

    private final ArsTaskRepository arsTaskRepository;

    public WorkflowService(ArsTaskRepository arsTaskRepository) {
        this.arsTaskRepository = arsTaskRepository;
    }

    /**
     * Submits an access request to the ARS Request Cart and initializes a task in MANAGER_PENDING stage.
     */
    @Transactional
    public WorkflowResponse submitRequest(WorkflowSubmitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Submit request payload cannot be null.");
        }
        if (request.userKey() == null || request.userKey() <= 0) {
            throw new IllegalArgumentException("User key must be a positive integer.");
        }
        if (request.requestedItem() == null || request.requestedItem().isBlank()) {
            throw new IllegalArgumentException("Requested item cannot be blank.");
        }
        if (request.justification() == null || request.justification().isBlank()) {
            throw new IllegalArgumentException("Justification cannot be blank.");
        }

        String item = request.requestedItem().trim();
        String justification = request.justification().trim();

        log.info("SAVIYNT WORKFLOW SERVICE: Submitting ARS request for userKey={} item='{}'", request.userKey(), item);

        ArsTaskEntity entity = new ArsTaskEntity();
        entity.setTasktype(1); // 1 = Add Access
        entity.setTaskdescription("Access Request: " + item);
        entity.setUserkey(request.userKey());
        entity.setStatus(STATUS_PENDING);
        entity.setProvisioningcomments(justification);
        entity.setWorkflowStage(STAGE_MANAGER_PENDING);

        ArsTaskEntity savedTask = arsTaskRepository.save(entity);

        log.info("SAVIYNT WORKFLOW SERVICE: Created ARS taskKey={} with stage={}", savedTask.getTaskkey(), STAGE_MANAGER_PENDING);

        return new WorkflowResponse(
                savedTask.getTaskkey(),
                savedTask.getTasktype() != null ? savedTask.getTasktype() : 1,
                savedTask.getTaskdescription(),
                savedTask.getUserkey(),
                savedTask.getStatus() != null ? savedTask.getStatus() : STATUS_PENDING,
                savedTask.getWorkflowStage(),
                savedTask.getProvisioningcomments(),
                savedTask.getApproverComments(),
                "Access request submitted successfully. ARS task initialized in MANAGER_PENDING stage."
        );
    }

    /**
     * Overloaded convenience method for direct parameter submission.
     */
    @Transactional
    public WorkflowResponse submitRequest(Long userKey, String requestedItem, String justification) {
        return submitRequest(new WorkflowSubmitRequest(userKey, requestedItem, justification));
    }

    /**
     * Evaluates and advances the multi-level approval workflow stage for a task atomically.
     * Enforces strict sequential transitions:
     * - Stage can transition to APP_OWNER_PENDING only if current stage is MANAGER_PENDING.
     * - Stage can transition to PROVISIONING_READY (status = 1) only if current stage is APP_OWNER_PENDING.
     * - Rejection sets stage to REJECTED and status to -1.
     */
    @Transactional
    public WorkflowResponse processApproval(long taskKey, WorkflowApprovalRequest request) {
        if (taskKey <= 0) {
            throw new IllegalArgumentException("Task key must be a positive integer.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Approval request payload cannot be null.");
        }
        if (request.action() == null || request.action().isBlank()) {
            throw new IllegalArgumentException("Approval action cannot be blank.");
        }
        if (request.approverRole() == null || request.approverRole().isBlank()) {
            throw new IllegalArgumentException("Approver role cannot be blank.");
        }

        String action = request.action().trim().toUpperCase();
        String role = request.approverRole().trim().toUpperCase();

        if (!ACTION_APPROVE.equals(action) && !ACTION_REJECT.equals(action)) {
            throw new IllegalArgumentException("Invalid action: '" + request.action() + "'. Allowed actions: APPROVE, REJECT.");
        }
        if (!ROLE_MANAGER.equals(role) && !ROLE_APP_OWNER.equals(role)) {
            throw new IllegalArgumentException("Invalid approver role: '" + request.approverRole() + "'. Allowed roles: MANAGER, APP_OWNER.");
        }

        ArsTaskEntity task = arsTaskRepository.findById(taskKey)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow task not found with key: " + taskKey));

        String currentStage = task.getWorkflowStage() != null ? task.getWorkflowStage().trim() : STAGE_MANAGER_PENDING;

        if (STAGE_PROVISIONING_READY.equalsIgnoreCase(currentStage) || STAGE_REJECTED.equalsIgnoreCase(currentStage)) {
            throw new IllegalStateException("Task " + taskKey + " is already in terminal stage: " + currentStage);
        }

        String nextStage;
        int nextStatus;
        String message;

        if (ACTION_REJECT.equals(action)) {
            // Rejection validation: Only authorized role for the current stage can reject
            if (STAGE_MANAGER_PENDING.equalsIgnoreCase(currentStage) && !ROLE_MANAGER.equals(role)) {
                throw new IllegalStateException("Task " + taskKey + " is in MANAGER_PENDING stage. Only MANAGER can reject at this stage, but received: " + role);
            }
            if (STAGE_APP_OWNER_PENDING.equalsIgnoreCase(currentStage) && !ROLE_APP_OWNER.equals(role)) {
                throw new IllegalStateException("Task " + taskKey + " is in APP_OWNER_PENDING stage. Only APP_OWNER can reject at this stage, but received: " + role);
            }

            nextStage = STAGE_REJECTED;
            nextStatus = STATUS_REJECTED;
            message = "Request rejected by " + role + ". Workflow stage updated to REJECTED with status -1.";
        } else {
            // Sequential state machine checks
            if (ROLE_MANAGER.equals(role)) {
                if (!STAGE_MANAGER_PENDING.equalsIgnoreCase(currentStage)) {
                    throw new IllegalStateException("Stage can transition to APP_OWNER_PENDING only if current stage is MANAGER_PENDING. Current stage: " + currentStage);
                }
                nextStage = STAGE_APP_OWNER_PENDING;
                nextStatus = STATUS_PENDING;
                message = "Manager approval recorded. Workflow stage updated to APP_OWNER_PENDING.";
            } else {
                if (!STAGE_APP_OWNER_PENDING.equalsIgnoreCase(currentStage)) {
                    throw new IllegalStateException("Stage can transition to PROVISIONING_READY only if current stage is APP_OWNER_PENDING. Current stage: " + currentStage);
                }
                nextStage = STAGE_PROVISIONING_READY;
                nextStatus = STATUS_PROVISIONING_READY;
                message = "App Owner approval recorded. Workflow stage updated to PROVISIONING_READY with status 1.";
            }
        }

        // Build audit comment entry
        String commentText = (request.comments() != null && !request.comments().isBlank())
                ? request.comments().trim()
                : "Decision: " + action;
        String newEntry = String.format("[%s - %s]: %s", role, action, commentText);
        String updatedApproverComments = (task.getApproverComments() == null || task.getApproverComments().isBlank())
                ? newEntry
                : task.getApproverComments() + " | " + newEntry;

        task.setWorkflowStage(nextStage);
        task.setStatus(nextStatus);
        task.setApproverComments(updatedApproverComments);

        ArsTaskEntity savedTask = arsTaskRepository.save(task);

        log.info("SAVIYNT WORKFLOW SERVICE: TaskKey={} transitioned from stage='{}' to stage='{}', status={}",
                taskKey, currentStage, nextStage, nextStatus);

        return new WorkflowResponse(
                savedTask.getTaskkey(),
                savedTask.getTasktype() != null ? savedTask.getTasktype() : 1,
                savedTask.getTaskdescription(),
                savedTask.getUserkey(),
                savedTask.getStatus() != null ? savedTask.getStatus() : nextStatus,
                savedTask.getWorkflowStage(),
                savedTask.getProvisioningcomments(),
                savedTask.getApproverComments(),
                message
        );
    }

    /**
     * Overloaded convenience method for direct parameter processing.
     */
    @Transactional
    public WorkflowResponse processApproval(long taskKey, String action, String approverRole, String comments) {
        return processApproval(taskKey, new WorkflowApprovalRequest(action, approverRole, comments));
    }

    /**
     * Retrieves an ARS workflow task by key.
     */
    @Transactional(readOnly = true)
    public WorkflowResponse getTaskById(long taskKey) {
        ArsTaskEntity task = arsTaskRepository.findById(taskKey)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow task not found with key: " + taskKey));

        return new WorkflowResponse(
                task.getTaskkey(),
                task.getTasktype() != null ? task.getTasktype() : 1,
                task.getTaskdescription(),
                task.getUserkey(),
                task.getStatus() != null ? task.getStatus() : 0,
                task.getWorkflowStage(),
                task.getProvisioningcomments(),
                task.getApproverComments(),
                "Task retrieved successfully."
        );
    }

    /**
     * Retrieves all workflow tasks.
     */
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getAllWorkflowTasks() {
        return arsTaskRepository.findAll().stream()
                .filter(t -> t.getWorkflowStage() != null)
                .map(task -> new WorkflowResponse(
                        task.getTaskkey() != null ? task.getTaskkey() : 0L,
                        task.getTasktype() != null ? task.getTasktype() : 1,
                        task.getTaskdescription(),
                        task.getUserkey(),
                        task.getStatus() != null ? task.getStatus() : 0,
                        task.getWorkflowStage(),
                        task.getProvisioningcomments(),
                        task.getApproverComments(),
                        "Task retrieved successfully."
                ))
                .toList();
    }
}
