package com.saviynt.sandbox.exception;

/**
 * Exception thrown when outbound provisioning to a target system fails or times out.
 */
public class TargetProvisioningException extends RuntimeException {

    public TargetProvisioningException(String message) {
        super(message);
    }

    public TargetProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
