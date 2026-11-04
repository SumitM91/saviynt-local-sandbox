package com.saviynt.sandbox.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saviynt.sandbox.dto.ProvisioningResult;
import com.saviynt.sandbox.entity.ArsTaskEntity;
import com.saviynt.sandbox.entity.ConnectionEntity;
import com.saviynt.sandbox.exception.TargetProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * REST Target Provisioning Connector (Module 8).
 * Parses connectionjson/addaccessjson, resolves dynamic placeholders, and dispatches HTTP POST
 * to the remote target system with a strict 2-second timeout.
 */
@Component("restTargetConnector")
public class RestTargetConnector implements TargetConnector {

    private static final Logger log = LoggerFactory.getLogger(RestTargetConnector.class);
    private static final int TIMEOUT_MILLIS = 2000;

    private final ObjectMapper objectMapper;

    public RestTargetConnector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ProvisioningResult provisionAccess(ArsTaskEntity task, ConnectionEntity connection) {
        log.info("Executing REST Target Provisioning for taskKey={}, userKey={}", task.getTaskkey(), task.getUserkey());

        String targetUrl = extractTargetUrl(connection != null ? connection.getConnectionjson() : null);
        String payloadTemplate = (connection != null && connection.getAddaccessjson() != null && !connection.getAddaccessjson().isBlank())
                ? connection.getAddaccessjson()
                : "{\"userKey\":\"${userKey}\",\"requestedItem\":\"${requestedItem}\"}";

        String effectivePayload = resolvePlaceholders(payloadTemplate, task);

        // Simulation / Mock URL support for testing environments
        if (targetUrl != null && targetUrl.startsWith("mock://")) {
            return handleMockUrl(targetUrl, effectivePayload, task);
        }

        if (targetUrl == null || targetUrl.isBlank()) {
            throw new TargetProvisioningException("Target REST URL is missing or empty in connectionjson.");
        }

        HttpURLConnection urlConnection = null;
        try {
            URL url = URI.create(targetUrl).toURL();
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(TIMEOUT_MILLIS);
            urlConnection.setReadTimeout(TIMEOUT_MILLIS);
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setDoOutput(true);

            byte[] input = effectivePayload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = urlConnection.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            int responseCode = urlConnection.getResponseCode();
            log.info("Target REST endpoint [{}] responded with HTTP status {}", targetUrl, responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                String responseBody = readStreamSafely(urlConnection.getInputStream());
                return ProvisioningResult.success(
                        "HTTP_" + responseCode,
                        "Successfully provisioned to REST target. Response: " + responseBody
                );
            } else if (responseCode >= 500) {
                String errorBody = readStreamSafely(urlConnection.getErrorStream());
                throw new TargetProvisioningException("Target REST endpoint returned 5xx server error: HTTP " + responseCode + " - " + errorBody);
            } else {
                String errorBody = readStreamSafely(urlConnection.getErrorStream());
                throw new TargetProvisioningException("Target REST endpoint returned non-success status: HTTP " + responseCode + " - " + errorBody);
            }

        } catch (TargetProvisioningException tpe) {
            throw tpe;
        } catch (Exception ex) {
            log.warn("REST Target outbound call failed for taskKey={}: {}", task.getTaskkey(), ex.getMessage());
            throw new TargetProvisioningException("Target connection failed or timed out: " + ex.getMessage(), ex);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private String extractTargetUrl(String connectionJson) {
        if (connectionJson == null || connectionJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(connectionJson);
            if (root.has("url")) {
                return root.get("url").asText();
            }
            if (root.has("host")) {
                String host = root.get("host").asText();
                int port = root.has("port") ? root.get("port").asInt() : 80;
                String path = root.has("path") ? root.get("path").asText() : "";
                return "http://" + host + ":" + port + path;
            }
        } catch (Exception ex) {
            log.warn("Unable to parse connectionjson for target URL: {}", ex.getMessage());
        }
        return null;
    }

    public String resolvePlaceholders(String template, ArsTaskEntity task) {
        if (template == null) {
            return "";
        }
        String userKey = task.getUserkey() != null ? String.valueOf(task.getUserkey()) : "";
        String requestedItem = task.getTaskdescription() != null ? task.getTaskdescription() : "";
        String accountName = task.getAccountname() != null ? task.getAccountname() : "";
        String taskKey = task.getTaskkey() != null ? String.valueOf(task.getTaskkey()) : "";

        return template
                .replace("${userKey}", userKey)
                .replace("${requestedItem}", requestedItem)
                .replace("${accountName}", accountName)
                .replace("${taskKey}", taskKey);
    }

    private ProvisioningResult handleMockUrl(String mockUrl, String payload, ArsTaskEntity task) {
        if (mockUrl.contains("fail") || mockUrl.contains("500") || mockUrl.contains("timeout")) {
            throw new TargetProvisioningException("Target REST endpoint mock failure: " + mockUrl);
        }
        return ProvisioningResult.success(
                "HTTP_200_MOCK",
                "Successfully provisioned to mock REST target. Payload: " + payload
        );
    }

    private String readStreamSafely(InputStream is) {
        if (is == null) {
            return "";
        }
        try {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
