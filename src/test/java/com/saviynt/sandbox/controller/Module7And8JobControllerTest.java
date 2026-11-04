package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.FailedTaskResponse;
import com.saviynt.sandbox.dto.TaskProvisioningSummary;
import com.saviynt.sandbox.dto.TaskRetryResponse;
import com.saviynt.sandbox.dto.WsRetryJobResult;
import com.saviynt.sandbox.service.Module7And8ProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Module7And8JobController.class)
class Module7And8JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Module7And8ProvisioningService provisioningService;

    @Test
    @DisplayName("POST /saviynt/module7-8/jobs/wsretry/trigger returns batch execution result")
    void testTriggerWsRetryJob() throws Exception {
        TaskProvisioningSummary summary = new TaskProvisioningSummary(
                31L, "Access Request: AWS_SecurityAdmin", "PROVISIONED", 1, 0, "SUCCESS", "Provisioned OK"
        );
        WsRetryJobResult jobResult = new WsRetryJobResult(
                "WSRETRY_DISPATCHER", 1, 1, 0, "WSRETRY batch finished", List.of(summary)
        );

        when(provisioningService.dispatchReadyTasks()).thenReturn(jobResult);

        mockMvc.perform(post("/saviynt/module7-8/jobs/wsretry/trigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("WSRETRY_DISPATCHER"))
                .andExpect(jsonPath("$.totalScanned").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.taskSummaries[0].taskKey").value(31));

        verify(provisioningService).dispatchReadyTasks();
    }

    @Test
    @DisplayName("POST /saviynt/module7-8/tasks/{taskKey}/retry executes manual retry")
    void testManualRetryTask() throws Exception {
        TaskRetryResponse retryResponse = new TaskRetryResponse(
                31L, "PROVISIONED", 1, 1, "SUCCESS", "Task successfully provisioned via manual retry"
        );

        when(provisioningService.manualRetryTask(31L)).thenReturn(retryResponse);

        mockMvc.perform(post("/saviynt/module7-8/tasks/31/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKey").value(31))
                .andExpect(jsonPath("$.workflowStage").value("PROVISIONED"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"));

        verify(provisioningService).manualRetryTask(31L);
    }

    @Test
    @DisplayName("GET /saviynt/module7-8/tasks/failed returns list of failed tasks")
    void testGetFailedTasks() throws Exception {
        FailedTaskResponse failedTask = new FailedTaskResponse(
                45L, 1, "AWS Admin Access", 101L, 501L, 10L, 2, "PROVISIONING_FAILED", 3,
                "Max retries exhausted", LocalDateTime.now()
        );

        when(provisioningService.getFailedTasks()).thenReturn(List.of(failedTask));

        mockMvc.perform(get("/saviynt/module7-8/tasks/failed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskKey").value(45))
                .andExpect(jsonPath("$[0].status").value(2))
                .andExpect(jsonPath("$[0].workflowStage").value("PROVISIONING_FAILED"))
                .andExpect(jsonPath("$[0].retryCount").value(3));

        verify(provisioningService).getFailedTasks();
    }
}
