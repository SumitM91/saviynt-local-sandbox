package com.saviynt.sandbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saviynt.sandbox.dto.WorkflowApprovalRequest;
import com.saviynt.sandbox.dto.WorkflowResponse;
import com.saviynt.sandbox.dto.WorkflowSubmitRequest;
import com.saviynt.sandbox.exception.GlobalExceptionHandler;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.service.WorkflowService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Module5And6WorkflowController.class)
@Import(GlobalExceptionHandler.class)
class Module5And6WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowService workflowService;

    @Test
    @DisplayName("POST /saviynt/module5-6/submit-request: Should submit request with JSON body")
    void testSubmitRequest_JsonBody() throws Exception {
        WorkflowSubmitRequest request = new WorkflowSubmitRequest(
                101L,
                "AWS_Production_Admin",
                "Deployment task"
        );

        WorkflowResponse mockResponse = new WorkflowResponse(
                5001L,
                1,
                "Access Request: AWS_Production_Admin",
                101L,
                0,
                "MANAGER_PENDING",
                "Deployment task",
                null,
                "Access request submitted successfully. ARS task initialized in MANAGER_PENDING stage."
        );

        when(workflowService.submitRequest(any(WorkflowSubmitRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/saviynt/module5-6/submit-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKey").value(5001))
                .andExpect(jsonPath("$.workflowStage").value("MANAGER_PENDING"))
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.taskDescription").value("Access Request: AWS_Production_Admin"));
    }

    @Test
    @DisplayName("POST /saviynt/module5-6/submit-request: Should accept query parameters fallback")
    void testSubmitRequest_QueryParams() throws Exception {
        WorkflowResponse mockResponse = new WorkflowResponse(
                5002L,
                1,
                "Access Request: Finance_Viewer",
                102L,
                0,
                "MANAGER_PENDING",
                "Audit work",
                null,
                "Access request submitted successfully. ARS task initialized in MANAGER_PENDING stage."
        );

        when(workflowService.submitRequest(any(WorkflowSubmitRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/saviynt/module5-6/submit-request")
                        .param("userKey", "102")
                        .param("requestedItem", "Finance_Viewer")
                        .param("justification", "Audit work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKey").value(5002))
                .andExpect(jsonPath("$.workflowStage").value("MANAGER_PENDING"));
    }

    @Test
    @DisplayName("POST /saviynt/module5-6/process-approval/{taskKey}: Should advance stage on approval")
    void testProcessApproval_Success() throws Exception {
        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "MANAGER", "Approved");

        WorkflowResponse mockResponse = new WorkflowResponse(
                5001L,
                1,
                "Access Request: AWS_Production_Admin",
                101L,
                0,
                "APP_OWNER_PENDING",
                "Deployment task",
                "[MANAGER - APPROVE]: Approved",
                "Manager approval recorded. Workflow stage updated to APP_OWNER_PENDING."
        );

        when(workflowService.processApproval(eq(5001L), any(WorkflowApprovalRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/saviynt/module5-6/process-approval/5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKey").value(5001))
                .andExpect(jsonPath("$.workflowStage").value("APP_OWNER_PENDING"))
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.approverComments", Matchers.containsString("[MANAGER - APPROVE]")));
    }

    @Test
    @DisplayName("POST /saviynt/module5-6/process-approval/{taskKey}: Should return 404 if task not found")
    void testProcessApproval_NotFound() throws Exception {
        WorkflowApprovalRequest request = new WorkflowApprovalRequest("APPROVE", "MANAGER", "Approved");

        when(workflowService.processApproval(eq(9999L), any(WorkflowApprovalRequest.class)))
                .thenThrow(new ResourceNotFoundException("Workflow task not found with key: 9999"));

        mockMvc.perform(post("/saviynt/module5-6/process-approval/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail", Matchers.containsString("9999")));
    }

    @Test
    @DisplayName("GET /saviynt/module5-6/tasks: Should return list of tasks")
    void testGetAllWorkflowTasks() throws Exception {
        WorkflowResponse mockResponse = new WorkflowResponse(
                5001L, 1, "Access Request", 101L, 0, "MANAGER_PENDING", "Justification", null, "Retrieved"
        );

        when(workflowService.getAllWorkflowTasks()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/saviynt/module5-6/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskKey").value(5001))
                .andExpect(jsonPath("$[0].workflowStage").value("MANAGER_PENDING"));
    }
}
