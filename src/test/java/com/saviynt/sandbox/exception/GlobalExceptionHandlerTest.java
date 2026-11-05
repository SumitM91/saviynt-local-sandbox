package com.saviynt.sandbox.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ServletWebRequest webRequest;
    private MockHttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/saviynt/test-endpoint");
        webRequest = new ServletWebRequest(servletRequest);
    }

    @Test
    @DisplayName("handleNotFound: Should return 404 ProblemDetail with WebRequest path")
    void testHandleNotFound_WithWebRequest() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found with ID 101");
        ProblemDetail problem = handler.handleNotFound(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("User not found with ID 101");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/resource-not-found"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleNotFound: Should support legacy single-arg call")
    void testHandleNotFound_SingleArg() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource missing");
        ProblemDetail problem = handler.handleNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("Resource missing");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleDatabaseError: Should return 500 ProblemDetail for DataAccessException")
    void testHandleDatabaseError() {
        CannotAcquireLockException ex = new CannotAcquireLockException("Lock deadlock detected");
        ProblemDetail problem = handler.handleDatabaseError(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Database Persistence Failure");
        assertThat(problem.getDetail()).contains("Lock deadlock detected");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/database-error"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleTargetProvisioningError: Should return 503 ProblemDetail for TargetProvisioningException")
    void testHandleTargetProvisioningError() {
        TargetProvisioningException ex = new TargetProvisioningException("Target timeout after 2000ms");
        ProblemDetail problem = handler.handleTargetProvisioningError(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Target Provisioning Failure");
        assertThat(problem.getDetail()).isEqualTo("Target timeout after 2000ms");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/target-provisioning-error"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleMalformedJson: Should return 400 ProblemDetail for HttpMessageNotReadableException")
    void testHandleMalformedJson() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");
        ProblemDetail problem = handler.handleMalformedJson(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Malformed Request Body");
        assertThat(problem.getDetail()).isEqualTo("Malformed JSON request body.");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/malformed-json"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgument: Should return 400 ProblemDetail for IllegalArgumentException")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter value");
        ProblemDetail problem = handler.handleIllegalArgument(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request Parameter");
        assertThat(problem.getDetail()).isEqualTo("Invalid parameter value");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/bad-request"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleIllegalState: Should return 400 ProblemDetail for IllegalStateException")
    void testHandleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Illegal state transition attempted");
        ProblemDetail problem = handler.handleIllegalState(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Illegal State Transition");
        assertThat(problem.getDetail()).isEqualTo("Illegal state transition attempted");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/illegal-state"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGenericException: Should return 500 ProblemDetail for generic Exception")
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected runtime glitch");
        ProblemDetail problem = handler.handleGenericException(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).contains("Unexpected runtime glitch");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/internal-error"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleValidationFailure: Should return 400 ProblemDetail with field error map")
    void testHandleValidationFailure() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
        bindingResult.addError(new FieldError("testObject", "username", "Username cannot be empty"));

        Method dummyMethod = this.getClass().getDeclaredMethod("setUp");
        MethodParameter methodParam = new MethodParameter(dummyMethod, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);

        ProblemDetail problem = handler.handleValidationFailure(ex, webRequest);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getDetail()).isEqualTo("Input validation failed.");
        assertThat(problem.getType()).isEqualTo(URI.create("https://saviynt.com/errors/validation-error"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/saviynt/test-endpoint"));
        assertThat(problem.getProperties()).containsKey("fieldErrors");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) problem.getProperties().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("username", "Username cannot be empty");
    }
}
