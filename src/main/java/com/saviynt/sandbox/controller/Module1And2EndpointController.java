package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.ConnectionTestResponse;
import com.saviynt.sandbox.dto.EndpointDetailResponse;
import com.saviynt.sandbox.service.Module1And2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Module 1 & 2: Identity Repository, Security Systems,
 * Endpoints & Target Connection Verification.
 */
@RestController
@RequestMapping("/saviynt/module1-2")
@Tag(name = "Module 1 & 2: Identity & Endpoint Governance",
     description = "Saviynt 3-Layer Application Architecture (Connection -> Security System -> Endpoint) & Live Target Connectivity Engine")
public class Module1And2EndpointController {

    private final Module1And2Service module1And2Service;

    public Module1And2EndpointController(Module1And2Service module1And2Service) {
        this.module1And2Service = module1And2Service;
    }

    // ==========================================
    // 1. LIST ALL CONFIGURED ENDPOINTS
    // ==========================================
    @GetMapping("/endpoints")
    @Operation(
            summary = "List All Configured Endpoints",
            description = "Retrieves all endpoints along with their 3-layer architecture hierarchy (Security System and Connection Type)."
    )
    @ApiResponse(responseCode = "200", description = "List of configured endpoints returned successfully")
    public ResponseEntity<List<EndpointDetailResponse>> getAllEndpoints() {
        return ResponseEntity.ok(module1And2Service.getAllEndpoints());
    }

    // ==========================================
    // 2. GET SPECIFIC ENDPOINT BY NAME
    // ==========================================
    @GetMapping("/endpoints/{endpointName}")
    @Operation(
            summary = "Get Endpoint Configuration by Name",
            description = "Fetches the full 3-layer architecture details for a specific endpoint by its unique endpointname."
    )
    @ApiResponse(responseCode = "200", description = "Endpoint configuration retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Endpoint not found with specified name")
    public ResponseEntity<EndpointDetailResponse> getEndpointByName(
            @Parameter(description = "Unique name of the endpoint (e.g. AWS_Production)", example = "AWS_Production", required = true)
            @PathVariable("endpointName") String endpointName
    ) {
        return ResponseEntity.ok(module1And2Service.getEndpointByName(endpointName));
    }

    // ==========================================
    // 3. EXECUTE TARGET CONNECTION VERIFICATION
    // ==========================================
    @PostMapping("/connections/{connectionKey}/test-connection")
    @Operation(
            summary = "Test Target System Connection",
            description = "Executes a live network ping / HTTP connectivity check to the target endpoint specified in connectionjson with a 2-second timeout."
    )
    @ApiResponse(responseCode = "200", description = "Target system reachable and connection verified",
            content = @Content(schema = @Schema(implementation = ConnectionTestResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid connection configuration or missing URL/host",
            content = @Content(schema = @Schema(implementation = ConnectionTestResponse.class)))
    @ApiResponse(responseCode = "503", description = "Target system unreachable, timed out, or connection failed",
            content = @Content(schema = @Schema(implementation = ConnectionTestResponse.class)))
    public ResponseEntity<ConnectionTestResponse> testConnection(
            @Parameter(description = "Connection primary key (e.g. 1)", example = "1", required = true)
            @PathVariable("connectionKey") Long connectionKey
    ) {
        ConnectionTestResponse response = module1And2Service.testConnection(connectionKey);
        if ("SUCCESS".equalsIgnoreCase(response.status())) {
            return ResponseEntity.ok(response);
        } else if (response.message() != null &&
                (response.message().contains("Invalid") || response.message().contains("Neither") || response.message().contains("empty"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
