package com.saviynt.sandbox.service;

import com.saviynt.sandbox.connector.ConnectorFactory;
import com.saviynt.sandbox.connector.TargetConnector;
import com.saviynt.sandbox.dto.FailedTaskResponse;
import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.dto.TaskRetryResponse;
import com.saviynt.sandbox.dto.WsRetryJobResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import com.saviynt.sandbox.exception.TargetProvisioningException;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import com.saviynt.sandbox.repository.ConnectionRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.SecuritySystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Module7And8ProvisioningServiceTest {

    @Mock
    private ArsTaskRepository arsTaskRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private SecuritySystemRepository securitySystemRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private ConnectorFactory connectorFactory;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TargetConnector targetConnector;

    @InjectMocks
    private Module7And8ProvisioningService provisioningService;

    private ArsTaskEntity readyTask;
    private ConnectionEntity testConnection;

    @BeforeEach
    void setUp() {
        readyTask = new ArsTaskEntity();
        readyTask.setTaskkey(100L);
        readyTask.setTasktype(1);
        readyTask.setTaskdescription("Access Request: AWS_SecurityAdmin");
        readyTask.setUserkey(101L);
        readyTask.setAccountkey(501L);
        readyTask.setEndpointkey(10L);
        readyTask.setStatus(1);
        readyTask.setWorkflowStage("PROVISIONING_READY");
        readyTask.setRetryCount(0);

        testConnection = new ConnectionEntity();
        testConnection.setConnectionkey(1L);
        testConnection.setConnectionname("AWS_REST_Conn");
        testConnection.setConnectiontype("REST");
        testConnection.setConnectionjson("{\"url\":\"mock://success\"}");
    }

    @Test
    @DisplayName("WSRETRY: Successful provisioning transitions task to PROVISIONED and links user_accounts")
    void testDispatchReadyTasks_Success() {
        when(arsTaskRepository.findByWorkflowStageAndStatus("PROVISIONING_READY", 1))
                .thenReturn(List.of(readyTask));
        when(connectionRepository.findAll())
                .thenReturn(List.of(testConnection));
        when(connectorFactory.getConnector(anyString()))
                .thenReturn(targetConnector);
        when(targetConnector.provisionAccess(eq(readyTask), any()))
                .thenReturn(ProvisioningResult.success("HTTP_200", "Provisioned OK"));

        WsRetryJobResult result = provisioningService.dispatchReadyTasks();

        assertThat(result.totalScanned()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(0);

        assertThat(readyTask.getWorkflowStage()).isEqualTo("PROVISIONED");
        assertThat(readyTask.getStatus()).isEqualTo(1);
        assertThat(readyTask.getTargetResponse()).contains("Successfully provisioned");

        verify(jdbcTemplate).update(contains("INSERT IGNORE INTO user_accounts"), eq(101L), eq(501L));
        verify(arsTaskRepository).save(readyTask);
    }

    @Test
    @DisplayName("WSRETRY: Target timeout increments retryCount and schedules exponential backoff while retaining status 1")
    void testDispatchReadyTasks_Timeout_IncrementsRetryCount() {
        when(arsTaskRepository.findByWorkflowStageAndStatus("PROVISIONING_READY", 1))
                .thenReturn(List.of(readyTask));
        when(connectionRepository.findAll())
                .thenReturn(List.of(testConnection));
        when(connectorFactory.getConnector(anyString()))
                .thenReturn(targetConnector);
        when(targetConnector.provisionAccess(eq(readyTask), any()))
                .thenThrow(new TargetProvisioningException("Connection timed out after 2000ms"));

        WsRetryJobResult result = provisioningService.dispatchReadyTasks();

        assertThat(result.totalScanned()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);

        assertThat(readyTask.getRetryCount()).isEqualTo(1);
        assertThat(readyTask.getStatus()).isEqualTo(1);
        assertThat(readyTask.getWorkflowStage()).isEqualTo("PROVISIONING_READY");
        assertThat(readyTask.getComments()).contains("[RETRY_COUNT: 1]");
        assertThat(readyTask.getComments()).contains("Exponential backoff: 2s");

        verify(arsTaskRepository).save(readyTask);
        verify(jdbcTemplate, never()).update(contains("INSERT IGNORE INTO user_accounts"), any(), any());
    }

    @Test
    @DisplayName("WSRETRY: Reaching max retries (3) transitions task to status = 2 and stage = PROVISIONING_FAILED")
    void testDispatchReadyTasks_MaxRetriesExceeded_TransitionsToFailed() {
        readyTask.setRetryCount(2); // 2 previous failures

        when(arsTaskRepository.findByWorkflowStageAndStatus("PROVISIONING_READY", 1))
                .thenReturn(List.of(readyTask));
        when(connectionRepository.findAll())
                .thenReturn(List.of(testConnection));
        when(connectorFactory.getConnector(anyString()))
                .thenReturn(targetConnector);
        when(targetConnector.provisionAccess(eq(readyTask), any()))
                .thenThrow(new TargetProvisioningException("Target host unreachable"));

        WsRetryJobResult result = provisioningService.dispatchReadyTasks();

        assertThat(result.totalScanned()).isEqualTo(1);
        assertThat(readyTask.getRetryCount()).isEqualTo(3);
        assertThat(readyTask.getStatus()).isEqualTo(2); // Action Required / Failed
        assertThat(readyTask.getWorkflowStage()).isEqualTo("PROVISIONING_FAILED");
        assertThat(readyTask.getComments()).contains("Max retries (3) exhausted");
        assertThat(readyTask.getTargetResponse()).contains("MAX_RETRIES_EXCEEDED");

        verify(arsTaskRepository).save(readyTask);
    }

    @Test
    @DisplayName("Manual Retry: Triggers administrative retry for failed task and updates state on success")
    void testManualRetryTask_Success() {
        readyTask.setStatus(2);
        readyTask.setWorkflowStage("PROVISIONING_FAILED");
        readyTask.setRetryCount(3);

        when(arsTaskRepository.findById(100L)).thenReturn(Optional.of(readyTask));
        when(connectionRepository.findAll()).thenReturn(List.of(testConnection));
        when(connectorFactory.getConnector(anyString())).thenReturn(targetConnector);
        when(targetConnector.provisionAccess(eq(readyTask), any()))
                .thenReturn(ProvisioningResult.success("HTTP_200", "Manual admin override"));

        TaskRetryResponse response = provisioningService.manualRetryTask(100L);

        assertThat(response.outcome()).isEqualTo("SUCCESS");
        assertThat(readyTask.getWorkflowStage()).isEqualTo("PROVISIONED");
        assertThat(readyTask.getStatus()).isEqualTo(1);
        verify(arsTaskRepository).save(readyTask);
    }

    @Test
    @DisplayName("Manual Retry: Rejects or acknowledges already provisioned task")
    void testManualRetryTask_AlreadyProvisioned() {
        readyTask.setStatus(1);
        readyTask.setWorkflowStage("PROVISIONED");

        when(arsTaskRepository.findById(100L)).thenReturn(Optional.of(readyTask));

        TaskRetryResponse response = provisioningService.manualRetryTask(100L);

        assertThat(response.outcome()).isEqualTo("ALREADY_PROVISIONED");
        verify(targetConnector, never()).provisionAccess(any(), any());
    }

    @Test
    @DisplayName("Get Failed Tasks: Filters only tasks with status = 2, retryCount >= 3, or PROVISIONING_FAILED")
    void testGetFailedTasks() {
        ArsTaskEntity failedTask1 = new ArsTaskEntity();
        failedTask1.setTaskkey(201L);
        failedTask1.setStatus(2);
        failedTask1.setWorkflowStage("PROVISIONING_FAILED");

        ArsTaskEntity failedTask2 = new ArsTaskEntity();
        failedTask2.setTaskkey(202L);
        failedTask2.setStatus(1);
        failedTask2.setWorkflowStage("PROVISIONING_READY");
        failedTask2.setRetryCount(3);

        ArsTaskEntity healthyTask = new ArsTaskEntity();
        healthyTask.setTaskkey(203L);
        healthyTask.setStatus(1);
        healthyTask.setWorkflowStage("PROVISIONING_READY");
        healthyTask.setRetryCount(0);

        when(arsTaskRepository.findAll()).thenReturn(List.of(failedTask1, failedTask2, healthyTask));

        List<FailedTaskResponse> failedTasks = provisioningService.getFailedTasks();

        assertThat(failedTasks).hasSize(2);
        assertThat(failedTasks).extracting(FailedTaskResponse::taskKey)
                .containsExactlyInAnyOrder(201L, 202L);
    }
}
