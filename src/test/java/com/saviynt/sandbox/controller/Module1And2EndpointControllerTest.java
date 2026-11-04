package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.ConnectionTestResponse;
import com.saviynt.sandbox.dto.EndpointDetailResponse;
import com.saviynt.sandbox.exception.GlobalExceptionHandler;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.service.Module1And2Service;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Module1And2EndpointController.class)
@Import(GlobalExceptionHandler.class)
class Module1And2EndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Module1And2Service module1And2Service;

    @Test
    @DisplayName("GET /saviynt/module1-2/endpoints: Should return list of all configured endpoints")
    void testGetAllEndpoints_Success() throws Exception {
        EndpointDetailResponse endpoint1 = new EndpointDetailResponse(
                10L, "AWS_Production", 1L, "AWS_SecSystem", 1L,
                "AWS_REST_Conn", "REST", "${user.firstname}.${user.lastname}.aws",
                "users.customproperty1 = accounts.customproperty1", "Active"
        );

        when(module1And2Service.getAllEndpoints()).thenReturn(List.of(endpoint1));

        mockMvc.perform(get("/saviynt/module1-2/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].endpointKey").value(10))
                .andExpect(jsonPath("$[0].endpointName").value("AWS_Production"))
                .andExpect(jsonPath("$[0].systemName").value("AWS_SecSystem"))
                .andExpect(jsonPath("$[0].connectionType").value("REST"));
    }

    @Test
    @DisplayName("GET /saviynt/module1-2/endpoints/{endpointName}: Should return endpoint details")
    void testGetEndpointByName_Success() throws Exception {
        EndpointDetailResponse endpoint = new EndpointDetailResponse(
                10L, "AWS_Production", 1L, "AWS_SecSystem", 1L,
                "AWS_REST_Conn", "REST", "${user.firstname}.${user.lastname}.aws",
                "users.customproperty1 = accounts.customproperty1", "Active"
        );

        when(module1And2Service.getEndpointByName("AWS_Production")).thenReturn(endpoint);

        mockMvc.perform(get("/saviynt/module1-2/endpoints/AWS_Production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpointKey").value(10))
                .andExpect(jsonPath("$.endpointName").value("AWS_Production"))
                .andExpect(jsonPath("$.connectionName").value("AWS_REST_Conn"));
    }

    @Test
    @DisplayName("GET /saviynt/module1-2/endpoints/{endpointName}: Should return 404 when endpoint not found")
    void testGetEndpointByName_NotFound() throws Exception {
        when(module1And2Service.getEndpointByName("NonExistent"))
                .thenThrow(new ResourceNotFoundException("Endpoint not found with name: NonExistent"));

        mockMvc.perform(get("/saviynt/module1-2/endpoints/NonExistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail", Matchers.containsString("NonExistent")));
    }

    @Test
    @DisplayName("POST /saviynt/module1-2/connections/{connectionKey}/test-connection: Should return 200 on SUCCESS")
    void testConnection_Success() throws Exception {
        ConnectionTestResponse successResponse = new ConnectionTestResponse(
                "SUCCESS",
                "Connection established successfully to target."
        );

        when(module1And2Service.testConnection(1L)).thenReturn(successResponse);

        mockMvc.perform(post("/saviynt/module1-2/connections/1/test-connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Connection established successfully to target."));
    }

    @Test
    @DisplayName("POST /saviynt/module1-2/connections/{connectionKey}/test-connection: Should return 503 on FAILED unreachable")
    void testConnection_Failed_ServiceUnavailable() throws Exception {
        ConnectionTestResponse failResponse = new ConnectionTestResponse(
                "FAILED",
                "Connection failed: Connection refused (Connection refused)"
        );

        when(module1And2Service.testConnection(1L)).thenReturn(failResponse);

        mockMvc.perform(post("/saviynt/module1-2/connections/1/test-connection"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Connection refused")));
    }

    @Test
    @DisplayName("POST /saviynt/module1-2/connections/{connectionKey}/test-connection: Should return 400 on invalid config")
    void testConnection_Failed_BadRequest() throws Exception {
        ConnectionTestResponse invalidResponse = new ConnectionTestResponse(
                "FAILED",
                "Connection failed: connectionjson is empty or not configured."
        );

        when(module1And2Service.testConnection(2L)).thenReturn(invalidResponse);

        mockMvc.perform(post("/saviynt/module1-2/connections/2/test-connection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message", Matchers.containsString("empty")));
    }
}
