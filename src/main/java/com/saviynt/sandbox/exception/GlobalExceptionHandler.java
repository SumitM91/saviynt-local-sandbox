package com.saviynt.sandbox.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler producing RFC 7807 compliant ProblemDetail representations.
 * Supports overloaded WebRequest context injection for rich error diagnostics and URI tracking.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return handleNotFound(ex, null);
    }

    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://saviynt.com/errors/resource-not-found"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDatabaseError(DataAccessException ex) {
        return handleDatabaseError(ex, null);
    }

    public ProblemDetail handleDatabaseError(DataAccessException ex, WebRequest request) {
        log.error("Database error encountered: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database error occurred while processing the request: " + ex.getMostSpecificCause().getMessage()
        );
        problem.setTitle("Database Persistence Failure");
        problem.setType(URI.create("https://saviynt.com/errors/database-error"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(TargetProvisioningException.class)
    public ProblemDetail handleTargetProvisioningError(TargetProvisioningException ex) {
        return handleTargetProvisioningError(ex, null);
    }

    public ProblemDetail handleTargetProvisioningError(TargetProvisioningException ex, WebRequest request) {
        log.warn("Target provisioning failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage()
        );
        problem.setTitle("Target Provisioning Failure");
        problem.setType(URI.create("https://saviynt.com/errors/target-provisioning-error"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        return handleMalformedJson(ex, null);
    }

    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed HTTP message payload: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request body.");
        problem.setTitle("Malformed Request Body");
        problem.setType(URI.create("https://saviynt.com/errors/malformed-json"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex) {
        return handleValidationFailure(ex, null);
    }

    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Input validation failed.");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://saviynt.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        problem.setProperty("fieldErrors", fieldErrors);
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return handleIllegalArgument(ex, null);
    }

    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request Parameter");
        problem.setType(URI.create("https://saviynt.com/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return handleIllegalState(ex, null);
    }

    public ProblemDetail handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Illegal State Transition");
        problem.setType(URI.create("https://saviynt.com/errors/illegal-state"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        return handleGenericException(ex, null);
    }

    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred: " + ex.getMessage()
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://saviynt.com/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        attachRequestInfo(problem, request);
        return problem;
    }

    private void attachRequestInfo(ProblemDetail problem, WebRequest request) {
        if (request != null) {
            String desc = request.getDescription(false);
            if (desc != null && desc.startsWith("uri=")) {
                problem.setInstance(URI.create(desc.substring(4)));
            }
        }
    }
}
