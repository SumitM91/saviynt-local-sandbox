package com.saviynt.sandbox.controller;

import com.saviynt.sandbox.dto.*;
import com.saviynt.sandbox.service.Module4JmlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Module 4: JML Lifecycle Controller (Joiner, Mover, Leaver)
 *
 * Exposes 3-Tier Enterprise REST endpoints for Saviynt IGA lifecycle simulation.
 */
@RestController
@RequestMapping("/saviynt/module4")
@Tag(name = "Module 4: JML Lifecycle Engine", description = "Saviynt Joiner, Mover, Leaver Lifecycle & ARS Provisioning Tasks Engine")
public class Module4JmlController {

    private final Module4JmlService jmlService;

    public Module4JmlController(Module4JmlService jmlService) {
        this.jmlService = jmlService;
    }

    // ==========================================
    // 1. JOINER SIMULATION
    // ==========================================
    @PostMapping("/simulate-joiner")
    @Operation(
            summary = "Simulate Joiner Onboarding & Birthright Access Provisioning",
            description = "Simulates a new HR user creation (employeetype='Full-Time', departmentname='Engineering', statuskey=1) " +
                    "and automatically inserts TaskType 3 (New Account) and TaskType 1 (Add Access - Birthright) into arstasks."
    )
    @ApiResponse(responseCode = "200", description = "Joiner successfully created and birthright tasks generated")
    public ResponseEntity<JoinerResponse> simulateJoiner(@RequestBody(required = false) JoinerRequest request) {
        return ResponseEntity.ok(jmlService.processJoiner(request));
    }

    // ==========================================
    // 2. MOVER SIMULATION
    // ==========================================
    @PostMapping("/simulate-mover")
    @Operation(
            summary = "Simulate Mover Department Transfer & Access Delta Evaluation",
            description = "Updates an existing user's department (e.g. from Finance to Engineering) and logs a Mover evaluation message."
    )
    @ApiResponse(responseCode = "200", description = "Mover transition executed and evaluation message logged")
    public ResponseEntity<MoverResponse> simulateMover(@RequestBody(required = false) MoverRequest request) {
        return ResponseEntity.ok(jmlService.processMover(request));
    }

    // ==========================================
    // 3. LEAVER SIMULATION
    // ==========================================
    @PostMapping("/simulate-leaver")
    @Operation(
            summary = "Simulate Leaver Termination & Immediate Deprovisioning",
            description = "Simulates a user termination by changing statuskey from 1 to 0 and automatically generates " +
                    "TaskType 12 (Disable Account) and TaskType 2 (Remove Access) entries in arstasks for all accounts linked via user_accounts."
    )
    @ApiResponse(responseCode = "200", description = "User terminated and deprovisioning tasks created")
    public ResponseEntity<LeaverResponse> simulateLeaver(@RequestBody(required = false) LeaverRequest request) {
        return ResponseEntity.ok(jmlService.processLeaver(request));
    }

    // ==========================================
    // 4. AUDIT & TASKS VIEW
    // ==========================================
    @GetMapping("/tasks")
    @Operation(
            summary = "View ARS Provisioning Tasks Queue",
            description = "Returns all provisioning tasks from the arstasks table for audit and verification."
    )
    @ApiResponse(responseCode = "200", description = "List of ARS provisioning tasks")
    public ResponseEntity<List<Map<String, Object>>> getAllArsTasks() {
        return ResponseEntity.ok(jmlService.getAllArsTasks());
    }
}
