package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.JoinerRequest;
import com.saviynt.sandbox.dto.JoinerResponse;
import com.saviynt.sandbox.dto.LeaverRequest;
import com.saviynt.sandbox.dto.LeaverResponse;
import com.saviynt.sandbox.dto.MoverRequest;
import com.saviynt.sandbox.dto.MoverResponse;
import com.saviynt.sandbox.entity.UserEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.AccountRepository;
import com.saviynt.sandbox.repository.AccountRepository.LinkedAccountEntity;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.EndpointRepository.EndpointEntity;
import com.saviynt.sandbox.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Module4JmlServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private ArsTaskRepository arsTaskRepository;

    @InjectMocks
    private Module4JmlService jmlService;

    // ==========================================
    // JOINER TESTS
    // ==========================================

    @Test
    @DisplayName("processJoiner: Should onboard user and generate TaskType 3 and TaskType 1")
    void testProcessJoiner_Success() {
        JoinerRequest request = new JoinerRequest(
                "john.doe", "John", "Doe", "john.doe@company.com",
                "Engineering", "Full-Time", "manager.one", 1L, "EMP999"
        );

        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.createUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyLong(), anyInt(), anyString()))
                .thenReturn(201L);
        when(endpointRepository.findFirstEndpoint()
        ).thenReturn(Optional.of(new EndpointEntity(10L, "AWS_Production")));
        when(arsTaskRepository.createTask(eq(3), anyString(), eq(201L), isNull(), eq(10L), eq(0), anyString()))
                .thenReturn(1001L);
        when(arsTaskRepository.createTask(eq(1), anyString(), eq(201L), isNull(), eq(10L), eq(0), anyString()))
                .thenReturn(1002L);

        JoinerResponse response = jmlService.processJoiner(request);

        assertThat(response.lifecycleEvent()).isEqualTo("JOINER");
        assertThat(response.userkey()).isEqualTo(201L);
        assertThat(response.username()).isEqualTo("john.doe");
        assertThat(response.statuskey()).isEqualTo(1);
        assertThat(response.provisioningTasks()).hasSize(2);
        assertThat(response.provisioningTasks().get(0).tasktype()).isEqualTo(3);
        assertThat(response.provisioningTasks().get(1).tasktype()).isEqualTo(1);

        verify(userRepository, times(1)).createUser(
                "john.doe", "John", "Doe", "john.doe@company.com",
                "Engineering", "Full-Time", "manager.one", 1L, 1, "EMP999"
        );
    }

    @Test
    @DisplayName("processJoiner: Should handle username collision by appending suffix")
    void testProcessJoiner_UsernameCollision() {
        JoinerRequest request = new JoinerRequest(
                "alex.smith", "Alex", "Smith", null,
                null, null, null, null, null
        );

        when(userRepository.existsByUsername("alex.smith")).thenReturn(true);
        when(userRepository.createUser(contains("alex.smith."), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyLong(), anyInt(), anyString()))
                .thenReturn(202L);
        when(endpointRepository.findFirstEndpoint())
                .thenReturn(Optional.of(new EndpointEntity(10L, "AWS_Production")));
        when(arsTaskRepository.createTask(anyInt(), anyString(), anyLong(), any(), anyLong(), anyInt(), anyString()))
                .thenReturn(1003L);

        JoinerResponse response = jmlService.processJoiner(request);

        assertThat(response.username()).startsWith("alex.smith.");
        assertThat(response.userkey()).isEqualTo(202L);
        assertThat(response.statuskey()).isEqualTo(1);
    }

    // ==========================================
    // MOVER TESTS
    // ==========================================

    @Test
    @DisplayName("processMover: Should update user department and log access delta")
    void testProcessMover_Success() {
        MoverRequest request = new MoverRequest("rahul.verma", "Engineering", "new.boss");

        UserEntity mockUser = new UserEntity(
                102L, "rahul.verma", "Rahul", "Verma", "rahul@hcl.com",
                "Finance", "Full-Time", "old.boss", 1L, 1, "EMP002"
        );
        when(userRepository.findByUsername("rahul.verma")).thenReturn(Optional.of(mockUser));
        when(userRepository.updateDepartmentAndManager(102L, "Engineering", "new.boss")).thenReturn(1);

        MoverResponse response = jmlService.processMover(request);

        assertThat(response.lifecycleEvent()).isEqualTo("MOVER");
        assertThat(response.userkey()).isEqualTo(102L);
        assertThat(response.previousDepartment()).isEqualTo("Finance");
        assertThat(response.newDepartment()).isEqualTo("Engineering");
        assertThat(response.newManager()).isEqualTo("new.boss");
        assertThat(response.moverEvaluation()).contains("Finance").contains("Engineering");

        verify(userRepository, times(1)).updateDepartmentAndManager(102L, "Engineering", "new.boss");
    }

    @Test
    @DisplayName("processMover: Should throw ResourceNotFoundException when no users exist")
    void testProcessMover_UserNotFound() {
        MoverRequest request = new MoverRequest("nonexistent.user", "Engineering", null);

        when(userRepository.findByUsername("nonexistent.user")).thenReturn(Optional.empty());
        when(userRepository.findFirstUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jmlService.processMover(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No users found to simulate Mover lifecycle.");
    }

    // ==========================================
    // LEAVER TESTS
    // ==========================================

    @Test
    @DisplayName("processLeaver: Should terminate user and generate TaskType 12 and TaskType 2 for linked accounts")
    void testProcessLeaver_Success() {
        LeaverRequest request = new LeaverRequest("sumit.mishra");

        UserEntity mockUser = new UserEntity(
                101L, "sumit.mishra", "Sumit", "Mishra", "sumit@hcl.com",
                "Engineering", "Full-Time", "boss", 1L, 1, "EMP001"
        );
        when(userRepository.findByUsername("sumit.mishra")).thenReturn(Optional.of(mockUser));
        when(userRepository.terminateUser(101L)).thenReturn(1);

        List<LinkedAccountEntity> linkedAccounts = List.of(
                new LinkedAccountEntity(501L, "sumit.mishra.aws", 10L, "AWS_Production", 1)
        );
        when(accountRepository.findAccountsByUserKey(101L)).thenReturn(linkedAccounts);
        when(arsTaskRepository.createTask(eq(12), anyString(), eq(101L), eq(501L), eq(10L), eq(0), anyString()))
                .thenReturn(2001L);
        when(arsTaskRepository.createTask(eq(2), anyString(), eq(101L), eq(501L), eq(10L), eq(0), anyString()))
                .thenReturn(2002L);
        when(accountRepository.updateAccountStatus(501L, 2)).thenReturn(1);

        LeaverResponse response = jmlService.processLeaver(request);

        assertThat(response.lifecycleEvent()).isEqualTo("LEAVER");
        assertThat(response.userkey()).isEqualTo(101L);
        assertThat(response.statuskey()).isZero();
        assertThat(response.linkedAccountsCount()).isEqualTo(1);
        assertThat(response.deprovisioningTasks()).hasSize(2);
        assertThat(response.deprovisioningTasks().get(0).tasktype()).isEqualTo(12);
        assertThat(response.deprovisioningTasks().get(1).tasktype()).isEqualTo(2);

        verify(userRepository, times(1)).terminateUser(101L);
        verify(accountRepository, times(1)).updateAccountStatus(501L, 2);
    }

    @Test
    @DisplayName("processLeaver: Should throw ResourceNotFoundException when no active users found")
    void testProcessLeaver_NotFound() {
        LeaverRequest request = new LeaverRequest("nonexistent.user");

        when(userRepository.findByUsername("nonexistent.user")).thenReturn(Optional.empty());
        when(userRepository.findFirstActiveUserWithLinkedAccounts()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jmlService.processLeaver(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active user found with linked accounts");
    }

    // ==========================================
    // AUDIT & QUEUE TESTS
    // ==========================================

    @Test
    @DisplayName("getAllArsTasks: Should return queue of tasks")
    void testGetAllArsTasks() {
        List<Map<String, Object>> mockTasks = List.of(
                Map.of("taskkey", 1L, "tasktype", 3, "tasktypeName", "New Account")
        );
        when(arsTaskRepository.findAllTasksWithDetails()).thenReturn(mockTasks);

        List<Map<String, Object>> tasks = jmlService.getAllArsTasks();

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().get("taskkey")).isEqualTo(1L);
        verify(arsTaskRepository, times(1)).findAllTasksWithDetails();
    }
}
