package com.saviynt.sandbox.connector;

import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveDirectoryConnectorTest {

    private ActiveDirectoryConnector adConnector;

    @BeforeEach
    void setUp() {
        adConnector = new ActiveDirectoryConnector();
    }

    @Test
    @DisplayName("AD UAC: Bitwise OR with 0x0002 disables account (512 -> 514)")
    void testDisableAccountUac() {
        int initialUac = 512; // 0x0200 (Normal account)
        int disabledUac = adConnector.disableAccountUac(initialUac);

        assertThat(disabledUac).isEqualTo(514); // 0x0202
        assertThat(adConnector.isAccountDisabled(disabledUac)).isTrue();
    }

    @Test
    @DisplayName("AD UAC: Bitwise AND NOT (~0x0002) enables account (514 -> 512)")
    void testEnableAccountUac() {
        int initialDisabledUac = 514; // 0x0202
        int enabledUac = adConnector.enableAccountUac(initialDisabledUac);

        assertThat(enabledUac).isEqualTo(512); // 0x0200
        assertThat(adConnector.isAccountDisabled(enabledUac)).isFalse();
    }

    @Test
    @DisplayName("AD Provisioning: TaskType 12 triggers Account Disable UAC bitwise transition")
    void testProvisionAccess_DisableTask() {
        ArsTaskEntity task = new ArsTaskEntity();
        task.setTaskkey(12L);
        task.setTasktype(12); // Disable Account
        task.setAccountname("sumit.mishra.ad");

        ConnectionEntity connection = new ConnectionEntity();
        connection.setConnectiontype("ActiveDirectory");

        ProvisioningResult result = adConnector.provisionAccess(task, connection);

        assertThat(result.success()).isTrue();
        assertThat(result.responsePayload()).isEqualTo("AD_UAC_514");
        assertThat(result.auditDetails()).contains("DISABLE").contains("512 (0x0200) -> 514 (0x0202)");
    }

    @Test
    @DisplayName("AD Provisioning: TaskType 1 triggers Account Enable / Access Add UAC bitwise transition")
    void testProvisionAccess_EnableTask() {
        ArsTaskEntity task = new ArsTaskEntity();
        task.setTaskkey(1L);
        task.setTasktype(1); // Add Access
        task.setAccountname("sumit.mishra.ad");

        ConnectionEntity connection = new ConnectionEntity();
        connection.setConnectiontype("ActiveDirectory");

        ProvisioningResult result = adConnector.provisionAccess(task, connection);

        assertThat(result.success()).isTrue();
        assertThat(result.responsePayload()).isEqualTo("AD_UAC_512");
        assertThat(result.auditDetails()).contains("ENABLE").contains("514 (0x0202) -> 512 (0x0200)");
    }
}
