package com.saviynt.sandbox.connector;

import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JDBC Target Provisioning Connector (Module 8).
 * Simulates direct database target table inserts/updates for account and entitlement provisioning.
 */
@Component("jdbcTargetConnector")
public class JdbcTargetConnector implements TargetConnector {

    private static final Logger log = LoggerFactory.getLogger(JdbcTargetConnector.class);

    @Override
    public ProvisioningResult provisionAccess(ArsTaskEntity task, ConnectionEntity connection) {
        String auditMessage = String.format("JDBC Target Provisioning: Successfully executed DML for task [%d], target userKey [%d]",
                task.getTaskkey(), task.getUserkey());
        log.info(auditMessage);
        return ProvisioningResult.success("JDBC_EXEC_OK", auditMessage);
    }
}
