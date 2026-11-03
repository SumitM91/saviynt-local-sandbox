package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.WorkflowApprovalRequest;
import com.saviynt.sandbox.dto.WorkflowResponse;
import com.saviynt.sandbox.dto.WorkflowSubmitRequest;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Enterprise Unit Test Suite for WorkflowService in Saviynt EIC Hybrid Backend Architecture.
 * Mocks ArsTaskRepository (Spring Data JPA) and verifies sequential stage machine invariants,
 * atomic transitions, and illegal transition guards.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private ArsTaskRepository arsTaskRepository;

    @InjectMocks
    private WorkflowService workflowService;

    // ==========================================
    // 1. REQUEST SUBMISSION & TASK CREATION
    // ==========================================

    @Test
    @DisplayName("submitRequest: Successful request submission and atomic JPA task creation")
    void testSubmitRequest_Success() {
        WorkflowSubmitRequest request = new WorkflowSubmitRequest(
                101L,
                "AWS_Production_Admin",
                "Need elevated access for production incident support"
        );

        when(arsTaskRepository.save(any(ArsTaskEntity.class))).thenAnswer(invocation -> {
            ArsTaskEntity entity = invocation.getArgument(0);
            entity.setTaskkey(5001L);
            return entity;
        });

        WorkflowResponse response = workflowService.submitRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.taskKey()).isEqualTo(5001L);
        assertThat(response.taskType()).isEqualTo(1);
        assertThat(response.taskDescription()).isEqualTo("Access Request: AWS_Production_Admin");
        assertThat(response.userKey()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo(0);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_MANAGER_PENDING);
        assertThat(response.comments()).isEqualTo("Need elevated access for production incident support");
        assertThat(response.approverComments()).isNull();
        assertThat(response.message()).contains(WorkflowService.STAGE_MANAGER_PENDING);

        ArgumentCaptor<ArsTaskEntity> captor = ArgumentCaptor.forClass(ArsTaskEntity.class);
        verify(arsTaskRepository, times(1)).save(captor.capture());
        ArsTaskEntity captured = captor.getValue();
        assertThat(captured.getUserkey()).isEqualTo(101L);
        assertThat(captured.getWorkflowStage()).isEqualTo(WorkflowService.STAGE_MANAGER_PENDING);
        assertThat(captured.getStatus()).isEqualTo(WorkflowService.STATUS_PENDING);
    }

    @Test
    @DisplayName("submitRequest: Should validate input parameters strictly")
    void testSubmitRequest_ValidationFailures() {
        assertThatThrownBy(() -> workflowService.submitRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload cannot be null");

        assertThatThrownBy(() -> workflowService.submitRequest(new WorkflowSubmitRequest(null, "Role", "Justification")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User key must be a positive integer");

        assertThatThrownBy(() -> workflowService.submitRequest(new WorkflowSubmitRequest(-5L, "Role", "Justification")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User key must be a positive integer");

        assertThatThrownBy(() -> workflowService.submitRequest(new WorkflowSubmitRequest(101L, "", "Justification")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requested item cannot be blank");

        assertThatThrownBy(() -> workflowService.submitRequest(new WorkflowSubmitRequest(101L, "Role", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Justification cannot be blank");
    }

    // ==========================================
    // 2. MANAGER APPROVAL -> APP_OWNER_PENDING
    // ==========================================

    @Test
    @DisplayName("processApproval: Manager approval moves stage to APP_OWNER_PENDING atomically")
    void testProcessApproval_ManagerApproval_MovesToAppOwnerPending() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setTasktype(1);
        existingTask.setTaskdescription("Access Request: AWS_Production_Admin");
        existingTask.setUserkey(101L);
        existingTask.setStatus(WorkflowService.STATUS_PENDING);
        existingTask.setProvisioningcomments("Need elevated access for production incident support");
        existingTask.setWorkflowStage(WorkflowService.STAGE_MANAGER_PENDING);

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));
        when(arsTaskRepository.save(any(ArsTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalRequest approvalRequest = new WorkflowApprovalRequest(
                "APPROVE",
                "MANAGER",
                "Manager approval granted - justified for sprint deployment"
        );

        WorkflowResponse response = workflowService.processApproval(taskKey, approvalRequest);

        assertThat(response).isNotNull();
        assertThat(response.taskKey()).isEqualTo(taskKey);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_APP_OWNER_PENDING);
        assertThat(response.status()).isEqualTo(0);
        assertThat(response.approverComments()).contains("[MANAGER - APPROVE]: Manager approval granted - justified for sprint deployment");
        assertThat(response.message()).contains(WorkflowService.STAGE_APP_OWNER_PENDING);

        ArgumentCaptor<ArsTaskEntity> captor = ArgumentCaptor.forClass(ArsTaskEntity.class);
        verify(arsTaskRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWorkflowStage()).isEqualTo(WorkflowService.STAGE_APP_OWNER_PENDING);
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
    }

    // ==========================================
    // 3. APP OWNER APPROVAL -> PROVISIONING_READY (status 1)
    // ==========================================

    @Test
    @DisplayName("processApproval: App Owner approval moves stage to PROVISIONING_READY with status 1")
    void testProcessApproval_AppOwnerApproval_MovesToProvisioningReadyWithStatus1() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setTasktype(1);
        existingTask.setTaskdescription("Access Request: AWS_Production_Admin");
        existingTask.setUserkey(101L);
        existingTask.setStatus(WorkflowService.STATUS_PENDING);
        existingTask.setProvisioningcomments("Need elevated access");
        existingTask.setWorkflowStage(WorkflowService.STAGE_APP_OWNER_PENDING);
        existingTask.setApproverComments("[MANAGER - APPROVE]: Manager approved");

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));
        when(arsTaskRepository.save(any(ArsTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalRequest approvalRequest = new WorkflowApprovalRequest(
                "APPROVE",
                "APP_OWNER",
                "Application Owner approved - access granted"
        );

        WorkflowResponse response = workflowService.processApproval(taskKey, approvalRequest);

        assertThat(response).isNotNull();
        assertThat(response.taskKey()).isEqualTo(taskKey);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_PROVISIONING_READY);
        assertThat(response.status()).isEqualTo(WorkflowService.STATUS_PROVISIONING_READY);
        assertThat(response.status()).isEqualTo(1);
        assertThat(response.approverComments()).contains("[MANAGER - APPROVE]: Manager approved");
        assertThat(response.approverComments()).contains("[APP_OWNER - APPROVE]: Application Owner approved - access granted");
        assertThat(response.message()).contains(WorkflowService.STAGE_PROVISIONING_READY);
        assertThat(response.message()).contains("status 1");

        ArgumentCaptor<ArsTaskEntity> captor = ArgumentCaptor.forClass(ArsTaskEntity.class);
        verify(arsTaskRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWorkflowStage()).isEqualTo(WorkflowService.STAGE_PROVISIONING_READY);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    // ==========================================
    // 4. REJECTION -> REJECTED (status -1)
    // ==========================================

    @Test
    @DisplayName("processApproval: Manager rejection moves stage to REJECTED with status -1")
    void testProcessApproval_ManagerRejection_MovesToRejectedWithStatusMinus1() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setTasktype(1);
        existingTask.setTaskdescription("Access Request: AWS_Production_Admin");
        existingTask.setUserkey(101L);
        existingTask.setStatus(WorkflowService.STATUS_PENDING);
        existingTask.setProvisioningcomments("Need elevated access");
        existingTask.setWorkflowStage(WorkflowService.STAGE_MANAGER_PENDING);

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));
        when(arsTaskRepository.save(any(ArsTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalRequest rejectionRequest = new WorkflowApprovalRequest(
                "REJECT",
                "MANAGER",
                "Rejected: Insufficient business justification"
        );

        WorkflowResponse response = workflowService.processApproval(taskKey, rejectionRequest);

        assertThat(response).isNotNull();
        assertThat(response.taskKey()).isEqualTo(taskKey);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_REJECTED);
        assertThat(response.status()).isEqualTo(WorkflowService.STATUS_REJECTED);
        assertThat(response.status()).isEqualTo(-1);
        assertThat(response.approverComments()).contains("[MANAGER - REJECT]: Rejected: Insufficient business justification");
        assertThat(response.message()).contains(WorkflowService.STAGE_REJECTED);
        assertThat(response.message()).contains("status -1");

        ArgumentCaptor<ArsTaskEntity> captor = ArgumentCaptor.forClass(ArsTaskEntity.class);
        verify(arsTaskRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWorkflowStage()).isEqualTo(WorkflowService.STAGE_REJECTED);
        assertThat(captor.getValue().getStatus()).isEqualTo(-1);
    }

    @Test
    @DisplayName("processApproval: App Owner rejection moves stage to REJECTED with status -1")
    void testProcessApproval_AppOwnerRejection_MovesToRejectedWithStatusMinus1() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setTasktype(1);
        existingTask.setTaskdescription("Access Request: AWS_Production_Admin");
        existingTask.setUserkey(101L);
        existingTask.setStatus(WorkflowService.STATUS_PENDING);
        existingTask.setProvisioningcomments("Need elevated access");
        existingTask.setWorkflowStage(WorkflowService.STAGE_APP_OWNER_PENDING);
        existingTask.setApproverComments("[MANAGER - APPROVE]: Manager approval granted");

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));
        when(arsTaskRepository.save(any(ArsTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalRequest rejectionRequest = new WorkflowApprovalRequest(
                "REJECT",
                "APP_OWNER",
                "Rejected: Segregation of duties conflict"
        );

        WorkflowResponse response = workflowService.processApproval(taskKey, rejectionRequest);

        assertThat(response).isNotNull();
        assertThat(response.taskKey()).isEqualTo(taskKey);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_REJECTED);
        assertThat(response.status()).isEqualTo(-1);
        assertThat(response.approverComments()).contains("[APP_OWNER - REJECT]: Rejected: Segregation of duties conflict");

        ArgumentCaptor<ArsTaskEntity> captor = ArgumentCaptor.forClass(ArsTaskEntity.class);
        verify(arsTaskRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWorkflowStage()).isEqualTo(WorkflowService.STAGE_REJECTED);
        assertThat(captor.getValue().getStatus()).isEqualTo(-1);
    }

    // ==========================================
    // 5. ILLEGAL STATE TRANSITION ATTEMPTS & GUARDS
    // ==========================================

    @Test
    @DisplayName("processApproval: App Owner cannot advance stage when task is still in MANAGER_PENDING")
    void testProcessApproval_IllegalTransition_AppOwnerPrematureApproval() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setWorkflowStage(WorkflowService.STAGE_MANAGER_PENDING);

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));

        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "APP_OWNER", "Premature approval");

        assertThatThrownBy(() -> workflowService.processApproval(taskKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stage can transition to PROVISIONING_READY only if current stage is APP_OWNER_PENDING");
    }

    @Test
    @DisplayName("processApproval: Manager cannot advance stage when task is in APP_OWNER_PENDING")
    void testProcessApproval_IllegalTransition_ManagerPostAppOwnerApproval() {
        long taskKey = 5001L;
        ArsTaskEntity existingTask = new ArsTaskEntity();
        existingTask.setTaskkey(taskKey);
        existingTask.setWorkflowStage(WorkflowService.STAGE_APP_OWNER_PENDING);

        when(arsTaskRepository.findById(taskKey)).thenReturn(Optional.of(existingTask));

        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "MANAGER", "Duplicate approval");

        assertThatThrownBy(() -> workflowService.processApproval(taskKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stage can transition to APP_OWNER_PENDING only if current stage is MANAGER_PENDING");
    }

    @Test
    @DisplayName("processApproval: Should reject transitions on already finalized tasks (PROVISIONING_READY / REJECTED)")
    void testProcessApproval_TerminalStageGuard() {
        long taskKey1 = 5001L;
        ArsTaskEntity completedTask = new ArsTaskEntity();
        completedTask.setTaskkey(taskKey1);
        completedTask.setWorkflowStage(WorkflowService.STAGE_PROVISIONING_READY);

        when(arsTaskRepository.findById(taskKey1)).thenReturn(Optional.of(completedTask));

        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "APP_OWNER", "Post-completion");

        assertThatThrownBy(() -> workflowService.processApproval(taskKey1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in terminal stage: PROVISIONING_READY");

        long taskKey2 = 5002L;
        ArsTaskEntity rejectedTask = new ArsTaskEntity();
        rejectedTask.setTaskkey(taskKey2);
        rejectedTask.setWorkflowStage(WorkflowService.STAGE_REJECTED);

        when(arsTaskRepository.findById(taskKey2)).thenReturn(Optional.of(rejectedTask));

        assertThatThrownBy(() -> workflowService.processApproval(taskKey2, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in terminal stage: REJECTED");
    }

    @Test
    @DisplayName("processApproval: Should throw ResourceNotFoundException when task does not exist")
    void testProcessApproval_TaskNotFound() {
        when(arsTaskRepository.findById(9999L)).thenReturn(Optional.empty());

        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "MANAGER", "OK");

        assertThatThrownBy(() -> workflowService.processApproval(9999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workflow task not found with key: 9999");
    }

    @Test
    @DisplayName("processApproval: Should throw IllegalArgumentException when invalid action or role provided")
    void testProcessApproval_InvalidActionOrRole() {
        WorkflowApprovalRequest badAction = new WorkflowApprovalRequest("DELEGATE", "MANAGER", "Delegate");
        assertThatThrownBy(() -> workflowService.processApproval(1L, badAction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Allowed actions: APPROVE, REJECT");

        WorkflowApprovalRequest badRole = new WorkflowApprovalRequest("APPROVE", "SECURITY_ADMIN", "Approved");
        assertThatThrownBy(() -> workflowService.processApproval(1L, badRole))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Allowed roles: MANAGER, APP_OWNER");
    }

    // ==========================================
    // 6. RETRIEVAL & AUDIT METHODS
    // ==========================================

    @Test
    @DisplayName("getTaskById: Should return task details when task exists")
    void testGetTaskById_Success() {
        ArsTaskEntity task = new ArsTaskEntity();
        task.setTaskkey(5001L);
        task.setTasktype(1);
        task.setTaskdescription("Access Request: AWS_Role");
        task.setUserkey(101L);
        task.setStatus(0);
        task.setWorkflowStage(WorkflowService.STAGE_MANAGER_PENDING);

        when(arsTaskRepository.findById(5001L)).thenReturn(Optional.of(task));

        WorkflowResponse response = workflowService.getTaskById(5001L);

        assertThat(response.taskKey()).isEqualTo(5001L);
        assertThat(response.workflowStage()).isEqualTo(WorkflowService.STAGE_MANAGER_PENDING);
    }

    @Test
    @DisplayName("getAllWorkflowTasks: Should return list of all workflow tasks")
    void testGetAllWorkflowTasks() {
        ArsTaskEntity task1 = new ArsTaskEntity();
        task1.setTaskkey(5001L);
        task1.setTasktype(1);
        task1.setStatus(0);
        task1.setWorkflowStage(WorkflowService.STAGE_MANAGER_PENDING);

        ArsTaskEntity task2 = new ArsTaskEntity();
        task2.setTaskkey(5002L);
        task2.setTasktype(1);
        task2.setStatus(1);
        task2.setWorkflowStage(WorkflowService.STAGE_PROVISIONING_READY);

        when(arsTaskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<WorkflowResponse> tasks = workflowService.getAllWorkflowTasks();

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).taskKey()).isEqualTo(5001L);
        assertThat(tasks.get(1).taskKey()).isEqualTo(5002L);
    }
}
