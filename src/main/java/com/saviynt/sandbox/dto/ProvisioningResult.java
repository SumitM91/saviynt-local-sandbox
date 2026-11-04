package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result representation for target system provisioning operations.
 */
@Schema(description = "Target system provisioning execution result")
public record ProvisioningResult(
        @Schema(description = "Indicates if the provisioning succeeded on the target system", example = "true")
        boolean success,

        @Schema(description = "HTTP or Protocol status payload returned by target", example = "HTTP_200_OK")
        String responsePayload,

        @Schema(description = "Detailed audit trail information", example = "Successfully created target entitlement")
        String auditDetails
) {
    public static ProvisioningResult success(String auditDetails) {
        return new ProvisioningResult(true, "SUCCESS", auditDetails);
    }

    public static ProvisioningResult success(String responsePayload, String auditDetails) {
        return new ProvisioningResult(true, responsePayload, auditDetails);
    }

    public static ProvisioningResult failure(String errorMessage) {
        return new ProvisioningResult(false, "FAILED", errorMessage);
    }
}
