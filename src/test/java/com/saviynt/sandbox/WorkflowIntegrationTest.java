package com.saviynt.sandbox;

import com.saviynt.sandbox.dto.WorkflowApprovalRequest;
import com.saviynt.sandbox.dto.WorkflowResponse;
import com.saviynt.sandbox.dto.WorkflowSubmitRequest;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import com.saviynt.sandbox.repository.CorrelationDao;
import com.saviynt.sandbox.service.Module1And2Service;
import com.saviynt.sandbox.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkflowIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ArsTaskRepository arsTaskRepository;

    @Autowired
    private CorrelationDao correlationDao;

    @Autowired
    private Module1And2Service module1And2Service;

    @Test
    @DisplayName("End-to-end integration: Submit -> Manager Approve -> App Owner Approve")
    void testFullWorkflowHappyPath() {
        // 1. Submit Request
        WorkflowSubmitRequest submitRequest = new WorkflowSubmitRequest(
                101L,
                "AWS_Production_Admin",
                "Integration test justification"
        );
        WorkflowResponse submitResponse = workflowService.submitRequest(submitRequest);

        assertThat(submitResponse.taskKey()).isPositive();
        assertThat(submitResponse.workflowStage()).isEqualTo("MANAGER_PENDING");
        assertThat(submitResponse.status()).isEqualTo(0);

        long taskKey = submitResponse.taskKey();

        // 2. Manager Approval
        WorkflowApprovalRequest managerApproval = new WorkflowApprovalRequest(
                "APPROVE",
                "MANAGER",
                "Approved by Vikram Boss"
        );
        WorkflowResponse managerResponse = workflowService.processApproval(taskKey, managerApproval);

        assertThat(managerResponse.workflowStage()).isEqualTo("APP_OWNER_PENDING");
        assertThat(managerResponse.status()).isEqualTo(0);
        assertThat(managerResponse.approverComments()).contains("[MANAGER - APPROVE]");

        // 3. App Owner Approval
        WorkflowApprovalRequest appOwnerApproval = new WorkflowApprovalRequest(
                "APPROVE",
                "APP_OWNER",
                "Approved by Application Owner"
        );
        WorkflowResponse appOwnerResponse = workflowService.processApproval(taskKey, appOwnerApproval);

        assertThat(appOwnerResponse.workflowStage()).isEqualTo("PROVISIONING_READY");
        assertThat(appOwnerResponse.status()).isEqualTo(1);
        assertThat(appOwnerResponse.approverComments()).contains("[APP_OWNER - APPROVE]");

        // 4. Verification in database
        WorkflowResponse persisted = workflowService.getTaskById(taskKey);
        assertThat(persisted.workflowStage()).isEqualTo("PROVISIONING_READY");
        assertThat(persisted.status()).isEqualTo(1);
    }

    @Test
    @DisplayName("End-to-end integration: Submit -> Rejection")
    void testFullWorkflowRejection() {
        WorkflowSubmitRequest submitRequest = new WorkflowSubmitRequest(
                101L,
                "AWS_Production_Admin",
                "Integration test rejection"
        );
        WorkflowResponse submitResponse = workflowService.submitRequest(submitRequest);
        long taskKey = submitResponse.taskKey();

        WorkflowApprovalRequest managerRejection = new WorkflowApprovalRequest(
                "REJECT",
                "MANAGER",
                "Manager rejected"
        );
        WorkflowResponse rejectionResponse = workflowService.processApproval(taskKey, managerRejection);

        assertThat(rejectionResponse.workflowStage()).isEqualTo("REJECTED");
        assertThat(rejectionResponse.status()).isEqualTo(-1);

        WorkflowResponse persisted = workflowService.getTaskById(taskKey);
        assertThat(persisted.workflowStage()).isEqualTo("REJECTED");
        assertThat(persisted.status()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Hybrid Architecture: Derived JPA queries findByWorkflowStageAndStatus and findByTasktype")
    void testDerivedJpaQueries() {
        workflowService.submitRequest(new WorkflowSubmitRequest(101L, "Role_A", "Justification A"));

        List<ArsTaskEntity> pendingTasks = arsTaskRepository.findByWorkflowStageAndStatus("MANAGER_PENDING", 0);
        assertThat(pendingTasks).isNotEmpty();

        List<ArsTaskEntity> addAccessTasks = arsTaskRepository.findByTasktype(1);
        assertThat(addAccessTasks).isNotEmpty();

        // Verify CorrelationDao bulk JdbcTemplate execution in same transaction
        var orphans = correlationDao.findOrphanAccounts();
        assertThat(orphans).isNotNull();
    }

    @Test
    @DisplayName("Module 1 & 2: Integration test querying existing endpoints and 3-tier mapping")
    void testModule1And2LiveHierarchy() {
        var endpoints = module1And2Service.getAllEndpoints();
        assertThat(endpoints).isNotEmpty();
        var aws = module1And2Service.getEndpointByName("AWS_Production");
        assertThat(aws.systemName()).isEqualTo("AWS_SecSystem");
        assertThat(aws.connectionName()).isEqualTo("AWS_REST_Conn");
        assertThat(aws.connectionType()).isEqualTo("REST");
    }
}
