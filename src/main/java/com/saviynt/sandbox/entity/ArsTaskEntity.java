package com.saviynt.sandbox.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for Saviynt ARS Provisioning and Workflow Tasks (arstasks).
 * Maps existing schema with zero DDL generation and supports WSRETRY engine attributes.
 */
@Entity
@Table(name = "arstasks")
public class ArsTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taskkey")
    private Long taskkey;

    @Column(name = "tasktype", nullable = false)
    private Integer tasktype;

    @Column(name = "taskdescription")
    private String taskdescription;

    @Column(name = "userkey")
    private Long userkey;

    @Column(name = "accountkey")
    private Long accountkey;

    @Column(name = "endpointkey")
    private Long endpointkey;

    @Transient
    private String endpoint;

    @Transient
    private String accountname;

    @Column(name = "status")
    private Integer status;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String provisioningcomments;

    @Column(name = "workflow_stage")
    private String workflowStage;

    @Column(name = "approver_comments", columnDefinition = "TEXT")
    private String approverComments;

    @Column(name = "createdate", insertable = false, updatable = false)
    private LocalDateTime createdate;

    // WSRETRY & Task Split Hierarchy Attributes (managed in entity to avoid DDL/schema breakage)
    @Transient
    private Integer retryCount = 0;

    @Transient
    private Long parentTaskId;

    @Transient
    private String targetResponse;

    public ArsTaskEntity() {
    }

    public ArsTaskEntity(Long taskkey, Integer tasktype, String taskdescription, Long userkey,
                         Long accountkey, Long endpointkey, Integer status, String provisioningcomments,
                         String workflowStage, String approverComments) {
        this.taskkey = taskkey;
        this.tasktype = tasktype;
        this.taskdescription = taskdescription;
        this.userkey = userkey;
        this.accountkey = accountkey;
        this.endpointkey = endpointkey;
        this.status = status;
        this.provisioningcomments = provisioningcomments;
        this.workflowStage = workflowStage;
        this.approverComments = approverComments;
    }

    public Long getTaskkey() {
        return taskkey;
    }

    public void setTaskkey(Long taskkey) {
        this.taskkey = taskkey;
    }

    public Integer getTasktype() {
        return tasktype;
    }

    public void setTasktype(Integer tasktype) {
        this.tasktype = tasktype;
    }

    public String getTaskdescription() {
        return taskdescription;
    }

    public void setTaskdescription(String taskdescription) {
        this.taskdescription = taskdescription;
    }

    public Long getUserkey() {
        return userkey;
    }

    public void setUserkey(Long userkey) {
        this.userkey = userkey;
    }

    public Long getAccountkey() {
        return accountkey;
    }

    public void setAccountkey(Long accountkey) {
        this.accountkey = accountkey;
    }

    public Long getEndpointkey() {
        return endpointkey;
    }

    public void setEndpointkey(Long endpointkey) {
        this.endpointkey = endpointkey;
    }

    public String getEndpoint() {
        return endpoint != null ? endpoint : (endpointkey != null ? String.valueOf(endpointkey) : null);
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        if (endpoint != null && endpoint.matches("\\d+")) {
            this.endpointkey = Long.parseLong(endpoint);
        }
    }

    public String getAccountname() {
        return accountname;
    }

    public void setAccountname(String accountname) {
        this.accountname = accountname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getProvisioningcomments() {
        return provisioningcomments;
    }

    public void setProvisioningcomments(String provisioningcomments) {
        this.provisioningcomments = provisioningcomments;
    }

    public String getComments() {
        return provisioningcomments;
    }

    public void setComments(String comments) {
        this.provisioningcomments = comments;
    }

    public String getWorkflowStage() {
        return workflowStage;
    }

    public void setWorkflowStage(String workflowStage) {
        this.workflowStage = workflowStage;
    }

    public String getWorkflow_stage() {
        return workflowStage;
    }

    public void setWorkflow_stage(String workflow_stage) {
        this.workflowStage = workflow_stage;
    }

    public String getApproverComments() {
        return approverComments;
    }

    public void setApproverComments(String approverComments) {
        this.approverComments = approverComments;
    }

    public String getApprover_comments() {
        return approverComments;
    }

    public void setApprover_comments(String approver_comments) {
        this.approverComments = approver_comments;
    }

    public LocalDateTime getCreatedate() {
        return createdate;
    }

    public void setCreatedate(LocalDateTime createdate) {
        this.createdate = createdate;
    }

    public int getRetryCount() {
        if (retryCount == null || retryCount == 0) {
            if (provisioningcomments != null && provisioningcomments.contains("[RETRY_COUNT:")) {
                try {
                    int start = provisioningcomments.indexOf("[RETRY_COUNT:") + 13;
                    int end = provisioningcomments.indexOf("]", start);
                    if (end > start) {
                        return Integer.parseInt(provisioningcomments.substring(start, end).trim());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return retryCount != null ? retryCount : 0;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount != null ? retryCount : 0;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getTargetResponse() {
        return targetResponse != null ? targetResponse : provisioningcomments;
    }

    public void setTargetResponse(String targetResponse) {
        this.targetResponse = targetResponse;
    }
}
