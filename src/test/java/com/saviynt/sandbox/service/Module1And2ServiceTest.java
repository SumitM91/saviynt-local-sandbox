package com.saviynt.sandbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saviynt.sandbox.dto.ConnectionTestResponse;
import com.saviynt.sandbox.dto.EndpointDetailResponse;
import com.saviynt.sandbox.entity.ConnectionEntity;
import com.saviynt.sandbox.entity.EndpointEntity;
import com.saviynt.sandbox.entity.SecuritySystemEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.ConnectionRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.SecuritySystemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Module1And2ServiceTest {

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private SecuritySystemRepository securitySystemRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private Module1And2Service service;

    @Test
    @DisplayName("getAllEndpoints: Should return list of endpoints with 3-tier hierarchy details")
    void testGetAllEndpoints_Success() {
        EndpointEntity endpoint = new EndpointEntity(
                10L, "AWS_Production", 1L,
                "${user.firstname}.${user.lastname}.aws",
                "users.customproperty1 = accounts.customproperty1",
                null
        );
        SecuritySystemEntity secSystem = new SecuritySystemEntity(
                1L, "AWS_SecSystem", 1L, "Manager_Approval_Workflow", 3
        );
        ConnectionEntity connection = new ConnectionEntity(
                1L, "AWS_REST_Conn", "REST",
                "{\"url\":\"http://localhost:8080/mock-target/oauth\"}",
                null, null, null
        );

        when(endpointRepository.findAll()).thenReturn(List.of(endpoint));
        when(securitySystemRepository.findById(1L)).thenReturn(Optional.of(secSystem));
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        List<EndpointDetailResponse> results = service.getAllEndpoints();

        assertThat(results).hasSize(1);
        EndpointDetailResponse result = results.getFirst();
        assertThat(result.endpointKey()).isEqualTo(10L);
        assertThat(result.endpointName()).isEqualTo("AWS_Production");
        assertThat(result.systemName()).isEqualTo("AWS_SecSystem");
        assertThat(result.connectionName()).isEqualTo("AWS_REST_Conn");
        assertThat(result.connectionType()).isEqualTo("REST");
    }

    @Test
    @DisplayName("getEndpointByName: Should fetch endpoint by name")
    void testGetEndpointByName_Success() {
        EndpointEntity endpoint = new EndpointEntity(
                10L, "AWS_Production", 1L,
                "${user.firstname}.${user.lastname}.aws",
                "users.customproperty1 = accounts.customproperty1",
                null
        );
        SecuritySystemEntity secSystem = new SecuritySystemEntity(
                1L, "AWS_SecSystem", 1L, "Manager_Approval_Workflow", 3
        );
        ConnectionEntity connection = new ConnectionEntity(
                1L, "AWS_REST_Conn", "REST",
                "{\"url\":\"http://localhost:8080/mock-target/oauth\"}",
                null, null, null
        );

        when(endpointRepository.findByEndpointname("AWS_Production")).thenReturn(Optional.of(endpoint));
        when(securitySystemRepository.findById(1L)).thenReturn(Optional.of(secSystem));
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        EndpointDetailResponse result = service.getEndpointByName("AWS_Production");

        assertThat(result).isNotNull();
        assertThat(result.endpointKey()).isEqualTo(10L);
        assertThat(result.endpointName()).isEqualTo("AWS_Production");
    }

    @Test
    @DisplayName("getEndpointByName: Should throw ResourceNotFoundException when endpoint missing")
    void testGetEndpointByName_NotFound() {
        when(endpointRepository.findByEndpointname("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEndpointByName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Endpoint not found with name: Unknown");
    }

    @Test
    @DisplayName("getEndpointByName: Should validate blank input")
    void testGetEndpointByName_Blank() {
        assertThatThrownBy(() -> service.getEndpointByName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint name cannot be blank");
    }

    @Test
    @DisplayName("testConnection: Should throw ResourceNotFoundException when connection missing")
    void testConnection_NotFound() {
        when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.testConnection(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found with key: 99");
    }

    @Test
    @DisplayName("testConnection: Should return FAILED when connectionjson is blank")
    void testConnection_BlankJson() {
        ConnectionEntity connection = new ConnectionEntity(
                1L, "Conn_Empty", "REST", null, null, null, null
        );
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        ConnectionTestResponse response = service.testConnection(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("connectionjson is empty");
    }

    @Test
    @DisplayName("testConnection: Should return FAILED when neither url nor host specified")
    void testConnection_MissingUrlAndHost() {
        ConnectionEntity connection = new ConnectionEntity(
                1L, "Conn_Invalid", "REST", "{\"authType\":\"oauth2\"}", null, null, null
        );
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        ConnectionTestResponse response = service.testConnection(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("Neither url nor host specified");
    }

    @Test
    @DisplayName("testConnection: Should return SUCCESS when HTTP ping succeeds")
    void testConnection_HttpSuccess() throws Exception {
        Module1And2Service spyService = spy(service);
        doNothing().when(spyService).pingHttpUrl(any(URL.class));

        ConnectionEntity connection = new ConnectionEntity(
                1L, "AWS_REST_Conn", "REST",
                "{\"url\":\"http://localhost:8080/mock-target/oauth\"}",
                null, null, null
        );
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        ConnectionTestResponse response = spyService.testConnection(1L);

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.message()).contains("Connection established successfully");
    }

    @Test
    @DisplayName("testConnection: Should return FAILED when HTTP ping throws IOException")
    void testConnection_HttpFailed() throws Exception {
        Module1And2Service spyService = spy(service);
        doThrow(new IOException("Connection refused")).when(spyService).pingHttpUrl(any(URL.class));

        ConnectionEntity connection = new ConnectionEntity(
                1L, "AWS_REST_Conn", "REST",
                "{\"url\":\"http://localhost:8080/mock-target/oauth\"}",
                null, null, null
        );
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        ConnectionTestResponse response = spyService.testConnection(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("Connection refused");
    }

    @Test
    @DisplayName("testConnection: Should return SUCCESS when Socket ping succeeds")
    void testConnection_SocketSuccess() throws Exception {
        Module1And2Service spyService = spy(service);
        doNothing().when(spyService).pingSocket(eq("localhost"), eq(8080));

        ConnectionEntity connection = new ConnectionEntity(
                1L, "TCP_Conn", "JDBC",
                "{\"host\":\"localhost\",\"port\":8080}",
                null, null, null
        );
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        ConnectionTestResponse response = spyService.testConnection(1L);

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.message()).contains("Connection established successfully");
    }
}
