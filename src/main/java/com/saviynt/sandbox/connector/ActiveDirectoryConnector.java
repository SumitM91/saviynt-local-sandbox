package com.saviynt.sandbox.connector;

import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Active Directory Target Connector (Module 8).
 * Simulates Active Directory userAccountControl (UAC) bitwise operations:
 * - Account Disable flag: 0x0002 (Bitwise OR to disable, Bitwise AND NOT to enable).
 * - Standard normal account: 0x0200 (512).
 * Formats and logs detailed bitwise compliance audit trails.
 */
@Component("activeDirectoryConnector")
public class ActiveDirectoryConnector implements TargetConnector {

    private static final Logger log = LoggerFactory.getLogger(ActiveDirectoryConnector.class);

    // Active Directory User Account Control (UAC) Flags
    public static final int UF_ACCOUNTDISABLE = 0x0002; // 2 in decimal
    public static final int UF_NORMAL_ACCOUNT = 0x0200;  // 512 in decimal

    @Override
    public ProvisioningResult provisionAccess(ArsTaskEntity task, ConnectionEntity connection) {
        String accountIdentifier = task.getAccountname() != null ? task.getAccountname() : "UserKey-" + task.getUserkey();
        int taskType = task.getTasktype() != null ? task.getTasktype() : 1;

        if (taskType == 12) { // Saviynt Standard 12 = Disable Account
            int initialUac = UF_NORMAL_ACCOUNT; // 512
            int updatedUac = disableAccountUac(initialUac); // 514

            String auditMessage = String.format(
                    "AD UAC Bitwise Audit [DISABLE]: Account '%s' UAC transitioned from %d (0x%04X) -> %d (0x%04X) via bitwise OR (0x%04X)",
                    accountIdentifier, initialUac, initialUac, updatedUac, updatedUac, UF_ACCOUNTDISABLE
            );
            log.info(auditMessage);

            return ProvisioningResult.success(
                    "AD_UAC_" + updatedUac,
                    auditMessage
            );
        } else {
            // TaskType 1 (Add Access), 3 (New Account), or general enable
            int initialUac = UF_NORMAL_ACCOUNT | UF_ACCOUNTDISABLE; // 514 (Disabled state)
            int updatedUac = enableAccountUac(initialUac); // 512 (Active state)

            String auditMessage = String.format(
                    "AD UAC Bitwise Audit [ENABLE]: Account '%s' UAC transitioned from %d (0x%04X) -> %d (0x%04X) via bitwise AND NOT (~0x%04X)",
                    accountIdentifier, initialUac, initialUac, updatedUac, updatedUac, UF_ACCOUNTDISABLE
            );
            log.info(auditMessage);

            return ProvisioningResult.success(
                    "AD_UAC_" + updatedUac,
                    auditMessage
            );
        }
    }

    /**
     * Applies Active Directory Bitwise OR with UF_ACCOUNTDISABLE (0x0002) to disable account.
     */
    public int disableAccountUac(int currentUac) {
        return currentUac | UF_ACCOUNTDISABLE;
    }

    /**
     * Applies Active Directory Bitwise AND NOT (~UF_ACCOUNTDISABLE) to clear disable flag and enable account.
     */
    public int enableAccountUac(int currentUac) {
        return currentUac & ~UF_ACCOUNTDISABLE;
    }

    /**
     * Evaluates if the AD account is currently marked disabled.
     */
    public boolean isAccountDisabled(int uac) {
        return (uac & UF_ACCOUNTDISABLE) != 0;
    }
}
