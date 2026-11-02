package com.saviynt.sandbox.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound: Should return 404 ProblemDetail for ResourceNotFoundException")
    void testHandleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        ProblemDetail problem = handler.handleNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("User not found");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleDatabaseError: Should return 500 ProblemDetail for DataAccessException")
    void testHandleDatabaseError() {
        CannotAcquireLockException ex = new CannotAcquireLockException("Lock deadlock detected");
        ProblemDetail problem = handler.handleDatabaseError(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Database Persistence Failure");
        assertThat(problem.getDetail()).contains("Lock deadlock detected");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleMalformedJson: Should return 400 ProblemDetail for HttpMessageNotReadableException")
    void testHandleMalformedJson() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");
        ProblemDetail problem = handler.handleMalformedJson(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Malformed Request Body");
        assertThat(problem.getDetail()).isEqualTo("Malformed JSON request body.");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgument: Should return 400 ProblemDetail for IllegalArgumentException")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter value");
        ProblemDetail problem = handler.handleIllegalArgument(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request Parameter");
        assertThat(problem.getDetail()).isEqualTo("Invalid parameter value");
    }

    @Test
    @DisplayName("handleGenericException: Should return 500 ProblemDetail for generic Exception")
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected runtime glitch");
        ProblemDetail problem = handler.handleGenericException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).contains("Unexpected runtime glitch");
    }
}
