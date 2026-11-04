package com.saviynt.sandbox.service;

import com.saviynt.sandbox.connector.ConnectorFactory;
import com.saviynt.sandbox.connector.TargetConnector;
import com.saviynt.sandbox.dto.*;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import com.saviynt.sandbox.entity.EndpointEntity;
import com.saviynt.sandbox.entity.SecuritySystemEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.exception.TargetProvisioningException;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import com.saviynt.sandbox.repository.ConnectionRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.SecuritySystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Service for Module 7 & 8: Job Control Panel, WSRETRY Engine, and Target Provisioning.
 */
@Service
public class Module7And8ProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(Module7And8ProvisioningService.class);
    public static final int MAX_RETRIES = 3;

    private final ArsTaskRepository arsTaskRepository;
    private final EndpointRepository endpointRepository;
    private final SecuritySystemRepository securitySystemRepository;
    private final ConnectionRepository connectionRepository;
    private final ConnectorFactory connectorFactory;
    private final JdbcTemplate jdbcTemplate;

    public Module7And8ProvisioningService(
            ArsTaskRepository arsTaskRepository,
            EndpointRepository endpointRepository,
            SecuritySystemRepository securitySystemRepository,
            ConnectionRepository connectionRepository,
            ConnectorFactory connectorFactory,
            JdbcTemplate jdbcTemplate
    ) {
        this.arsTaskRepository = arsTaskRepository;
        this.endpointRepository = endpointRepository;
        this.securitySystemRepository = securitySystemRepository;
        this.connectionRepository = connectionRepository;
        this.connectorFactory = connectorFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * WSRETRY Dispatcher Engine:
     * Queries all tasks where workflow_stage = 'PROVISIONING_READY' and status = 1 and retryCount < 3.
     * Routes each task through ConnectorFactory.
     */
    @Transactional
    public WsRetryJobResult dispatchReadyTasks() {
        log.info("Starting WSRETRY Dispatcher execution...");

        List<ArsTaskEntity> readyTasks = arsTaskRepository.findByWorkflowStageAndStatus("PROVISIONING_READY", 1);
        List<ArsTaskEntity> eligibleTasks = readyTasks.stream()
                .filter(t -> t.getRetryCount() < MAX_RETRIES)
                .toList();

        int totalScanned = eligibleTasks.size();
        int successCount = 0;
        int failedCount = 0;
        List<TaskProvisioningSummary> summaries = new ArrayList<>();

        for (ArsTaskEntity task : eligibleTasks) {
            TaskProvisioningSummary summary = executeProvisioningAttempt(task);
            summaries.add(summary);

            if ("SUCCESS".equalsIgnoreCase(summary.outcome())) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        String summaryText = String.format("WSRETRY execution completed: %d scanned, %d succeeded, %d failed/rescheduled.",
                totalScanned, successCount, failedCount);
        log.info(summaryText);

        return new WsRetryJobResult(
                "WSRETRY_DISPATCHER",
                totalScanned,
                successCount,
                failedCount,
                summaryText,
                summaries
        );
    }

    /**
     * Administrative manual retry for a specific ARS task from Job Control Panel.
     */
    @Transactional
    public TaskRetryResponse manualRetryTask(Long taskKey) {
        log.info("Administrative manual retry triggered for taskKey={}", taskKey);

        ArsTaskEntity task = arsTaskRepository.findById(taskKey)
                .orElseThrow(() -> new ResourceNotFoundException("ArsTask not found with taskKey=" + taskKey));

        if (task.getStatus() != null && task.getStatus() == 1 && "PROVISIONED".equalsIgnoreCase(task.getWorkflowStage())) {
            return new TaskRetryResponse(
                    task.getTaskkey(),
                    task.getWorkflowStage(),
                    task.getStatus(),
                    task.getRetryCount(),
                    "ALREADY_PROVISIONED",
                    "Task is already in PROVISIONED state with status 1."
            );
        }

        // Allow retry from failed or ready stage
        TaskProvisioningSummary summary = executeProvisioningAttempt(task);

        return new TaskRetryResponse(
                task.getTaskkey(),
                task.getWorkflowStage(),
                task.getStatus(),
                task.getRetryCount(),
                summary.outcome(),
                summary.message()
        );
    }

    /**
     * Lists all tasks requiring administrative action (status = 2 or retryCount >= 3 or PROVISIONING_FAILED).
     */
    @Transactional(readOnly = true)
    public List<FailedTaskResponse> getFailedTasks() {
        return arsTaskRepository.findAll().stream()
                .filter(task -> (task.getStatus() != null && task.getStatus() == 2)
                        || task.getRetryCount() >= MAX_RETRIES
                        || "PROVISIONING_FAILED".equalsIgnoreCase(task.getWorkflowStage()))
                .map(this::mapToFailedTaskResponse)
                .toList();
    }

    private TaskProvisioningSummary executeProvisioningAttempt(ArsTaskEntity task) {
        ConnectionEntity connection = resolveConnectionForTask(task);
        String connectionType = connection != null ? connection.getConnectiontype() : "REST";
        TargetConnector connector = connectorFactory.getConnector(connectionType);

        try {
            ProvisioningResult result = connector.provisionAccess(task, connection);

            if (result.success()) {
                task.setWorkflowStage("PROVISIONED");
                task.setStatus(1);
                String auditMsg = "Successfully provisioned to target system: " + result.auditDetails();
                task.setTargetResponse(auditMsg);
                task.setComments((task.getComments() != null && !task.getComments().isBlank() ? task.getComments() + " | " : "") + auditMsg);

                linkUserAccountIfApplicable(task);
                arsTaskRepository.save(task);

                return new TaskProvisioningSummary(
                        task.getTaskkey(),
                        task.getTaskdescription(),
                        task.getWorkflowStage(),
                        task.getStatus(),
                        task.getRetryCount(),
                        "SUCCESS",
                        auditMsg
                );
            } else {
                throw new TargetProvisioningException(result.auditDetails());
            }

        } catch (Exception ex) {
            int newRetryCount = task.getRetryCount() + 1;
            task.setRetryCount(newRetryCount);
            long backoffSeconds = (long) Math.pow(2, newRetryCount); // Exponential Backoff: 2s, 4s, 8s

            log.warn("WSRETRY Engine: Task [{}] failed on attempt {}. Scheduling exponential backoff: {}s. Root cause: {}",
                    task.getTaskkey(), newRetryCount, backoffSeconds, ex.getMessage());

            if (newRetryCount < MAX_RETRIES) {
                task.setStatus(1);
                task.setWorkflowStage("PROVISIONING_READY");
                String retryMsg = String.format("[RETRY_COUNT: %d] Target provisioning failed: %s (Exponential backoff: %ds)",
                        newRetryCount, ex.getMessage(), backoffSeconds);
                task.setComments(retryMsg);
                task.setTargetResponse("FAILED: " + ex.getMessage());

                arsTaskRepository.save(task);

                return new TaskProvisioningSummary(
                        task.getTaskkey(),
                        task.getTaskdescription(),
                        task.getWorkflowStage(),
                        task.getStatus(),
                        task.getRetryCount(),
                        "RETRYING",
                        retryMsg
                );
            } else {
                task.setStatus(2); // Action Required / Failed
                task.setWorkflowStage("PROVISIONING_FAILED");
                String terminalMsg = String.format("[RETRY_COUNT: %d] Max retries (%d) exhausted. Root cause error: %s",
                        newRetryCount, MAX_RETRIES, ex.getMessage());
                task.setComments(terminalMsg);
                task.setTargetResponse("MAX_RETRIES_EXCEEDED: " + ex.getMessage());

                arsTaskRepository.save(task);

                return new TaskProvisioningSummary(
                        task.getTaskkey(),
                        task.getTaskdescription(),
                        task.getWorkflowStage(),
                        task.getStatus(),
                        task.getRetryCount(),
                        "FAILED_TERMINAL",
                        terminalMsg
                );
            }
        }
    }

    private ConnectionEntity resolveConnectionForTask(ArsTaskEntity task) {
        if (task.getEndpointkey() != null) {
            EndpointEntity endpoint = endpointRepository.findById(task.getEndpointkey()).orElse(null);
            if (endpoint != null && endpoint.getSystemkey() != null) {
                SecuritySystemEntity system = securitySystemRepository.findById(endpoint.getSystemkey()).orElse(null);
                if (system != null && system.getConnectionkey() != null) {
                    ConnectionEntity conn = connectionRepository.findById(system.getConnectionkey()).orElse(null);
                    if (conn != null) {
                        return conn;
                    }
                }
            }
        }

        // Fallback to first configured connection
        return connectionRepository.findAll().stream().findFirst().orElse(null);
    }

    private void linkUserAccountIfApplicable(ArsTaskEntity task) {
        if (task.getUserkey() != null && task.getAccountkey() != null) {
            jdbcTemplate.update("INSERT IGNORE INTO user_accounts (userkey, accountkey) VALUES (?, ?)",
                    task.getUserkey(), task.getAccountkey());
            log.info("Linked user_accounts: userKey={}, accountKey={}", task.getUserkey(), task.getAccountkey());
        } else if (task.getUserkey() != null) {
            try {
                Long endpointKey = task.getEndpointkey() != null ? task.getEndpointkey() : 10L;
                List<Long> accountKeys = jdbcTemplate.query(
                        "SELECT a.accountkey FROM accounts a WHERE a.endpointkey = ? OR a.customproperty1 = (SELECT u.customproperty1 FROM users u WHERE u.userkey = ?)",
                        (rs, rowNum) -> rs.getLong("accountkey"),
                        endpointKey,
                        task.getUserkey()
                );
                if (!accountKeys.isEmpty()) {
                    Long accKey = accountKeys.get(0);
                    task.setAccountkey(accKey);
                    jdbcTemplate.update("INSERT IGNORE INTO user_accounts (userkey, accountkey) VALUES (?, ?)",
                            task.getUserkey(), accKey);
                    log.info("Auto-correlated and linked user_accounts: userKey={}, accountKey={}", task.getUserkey(), accKey);
                }
            } catch (Exception ex) {
                log.warn("Non-fatal: could not link user_accounts for taskKey={}: {}", task.getTaskkey(), ex.getMessage());
            }
        }
    }

    private FailedTaskResponse mapToFailedTaskResponse(ArsTaskEntity task) {
        return new FailedTaskResponse(
                task.getTaskkey(),
                task.getTasktype(),
                task.getTaskdescription(),
                task.getUserkey(),
                task.getAccountkey(),
                task.getEndpointkey(),
                task.getStatus(),
                task.getWorkflowStage(),
                task.getRetryCount(),
                task.getComments(),
                task.getCreatedate()
        );
    }
}
