package com.saviynt.sandbox.connector;

import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * LDAP Target Provisioning Connector (Module 8).
 * Simulates LDAP inetOrgPerson / groupOfUniqueNames attribute modifications.
 */
@Component("ldapTargetConnector")
public class LdapTargetConnector implements TargetConnector {

    private static final Logger log = LoggerFactory.getLogger(LdapTargetConnector.class);

    @Override
    public ProvisioningResult provisionAccess(ArsTaskEntity task, ConnectionEntity connection) {
        String dn = "uid=" + (task.getUserkey() != null ? task.getUserkey() : "unknown") + ",ou=users,dc=saviynt,dc=local";
        String auditMessage = String.format("LDAP Provisioning: Successfully updated DN [%s] with role/entitlement [%s]",
                dn, task.getTaskdescription());
        log.info(auditMessage);
        return ProvisioningResult.success("LDAP_MOD_OK", auditMessage);
    }
}
