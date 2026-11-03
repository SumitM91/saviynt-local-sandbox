package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.ArsTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Spring Data JPA Repository for ARS Provisioning and Multi-Level Workflow tasks.
 */
@Repository
public interface ArsTaskRepository extends JpaRepository<ArsTaskEntity, Long> {

    List<ArsTaskEntity> findByWorkflowStageAndStatus(String workflowStage, Integer status);

    List<ArsTaskEntity> findByTasktype(Integer tasktype);

    @Query(value = """
        SELECT 
            t.taskkey,
            t.tasktype,
            CASE t.tasktype
                WHEN 1 THEN 'Add Access (Birthright)'
                WHEN 2 THEN 'Remove Access'
                WHEN 3 THEN 'New Account'
                WHEN 12 THEN 'Disable Account'
                ELSE CONCAT('TaskType-', t.tasktype)
            END AS tasktypeName,
            t.taskdescription,
            t.userkey,
            u.username,
            t.accountkey,
            a.name AS accountname,
            t.endpointkey,
            e.endpointname,
            t.status,
            t.comments,
            t.createdate
        FROM arstasks t
        LEFT JOIN users u ON t.userkey = u.userkey
        LEFT JOIN accounts a ON t.accountkey = a.accountkey
        LEFT JOIN endpoints e ON t.endpointkey = e.endpointkey
        ORDER BY t.taskkey DESC
        """, nativeQuery = true)
    List<Map<String, Object>> findAllTasksWithDetails();

    default long createTask(int taskType, String taskDescription, Long userKey, Long accountKey, Long endpointKey, int status, String comments) {
        ArsTaskEntity entity = new ArsTaskEntity();
        entity.setTasktype(taskType);
        entity.setTaskdescription(taskDescription);
        entity.setUserkey(userKey);
        entity.setAccountkey(accountKey);
        entity.setEndpointkey(endpointKey);
        entity.setStatus(status);
        entity.setProvisioningcomments(comments);
        entity.setWorkflowStage("MANAGER_PENDING");
        return save(entity).getTaskkey();
    }
}
