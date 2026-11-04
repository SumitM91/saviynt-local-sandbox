package com.saviynt.sandbox.connector;

import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;

/**
 * Enterprise SPI for Saviynt target provisioning connectors.
 */
public interface TargetConnector {

    /**
     * Executes provisioning operation against target endpoint system.
     *
     * @param task the ARS task requesting provisioning
     * @param connection the target connection configuration
     * @return provisioning result
     */
    ProvisioningResult provisionAccess(ArsTaskEntity task, ConnectionEntity connection);
}
