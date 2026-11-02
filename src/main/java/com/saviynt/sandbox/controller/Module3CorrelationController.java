package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.CorrelationJobResponse;
import com.saviynt.sandbox.dto.OrphanAccountResponse;
import com.saviynt.sandbox.dto.TerminatedViolationResponse;
import com.saviynt.sandbox.service.Module3CorrelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/saviynt/module3")
@Tag(name = "Module 3: Correlation & Analytics Engine", description = "Saviynt Account Correlation Rules & Orphan Analytics")
public class Module3CorrelationController {

    private final Module3CorrelationService correlationService;

    public Module3CorrelationController(Module3CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    // 1. Saviynt Analytics V2: Find Uncorrelated / Orphan Accounts
    @GetMapping("/orphans")
    @Operation(
            summary = "Find Uncorrelated / Orphan Accounts",
            description = "Executes Saviynt Analytics V2 query to detect accounts not currently linked in user_accounts."
    )
    @ApiResponse(responseCode = "200", description = "List of orphan/uncorrelated accounts returned successfully")
    public ResponseEntity<List<OrphanAccountResponse>> getOrphanAccounts() {
        return ResponseEntity.ok(correlationService.getOrphanAccounts());
    }

    // 2. Saviynt Account Correlation Rule Engine
    @PostMapping("/run-correlation")
    @Operation(
            summary = "Run Account Correlation Job",
            description = "Correlates unlinked accounts with users based on Employee ID (customproperty1) and populates user_accounts."
    )
    @ApiResponse(responseCode = "200", description = "Correlation job completed successfully")
    public ResponseEntity<CorrelationJobResponse> runAccountCorrelationJob() {
        return ResponseEntity.ok(correlationService.runAccountCorrelationJob());
    }

    // 3. Compliance Audit: Terminated Users who STILL have Active Accounts
    @GetMapping("/terminated-active-violations")
    @Operation(
            summary = "Audit Terminated Users with Active Accounts",
            description = "Identifies high-risk compliance violations where terminated users (statuskey=0) still possess active target accounts (status=1)."
    )
    @ApiResponse(responseCode = "200", description = "Audit query executed successfully")
    public ResponseEntity<List<TerminatedViolationResponse>> getTerminatedUsersWithActiveAccounts() {
        return ResponseEntity.ok(correlationService.getTerminatedUsersWithActiveAccounts());
    }
}
