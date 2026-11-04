package com.saviynt.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saviynt.sandbox.dto.ConnectionTestResponse;
import com.saviynt.sandbox.dto.EndpointDetailResponse;
import com.saviynt.sandbox.entity.ConnectionEntity;
import com.saviynt.sandbox.entity.EndpointEntity;
import com.saviynt.sandbox.entity.SecuritySystemEntity;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.ConnectionRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.SecuritySystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Enterprise Service for Module 1 & 2: Identity Repository, Security Systems,
 * Endpoints & Target Connection Verification.
 */
@Service
public class Module1And2Service {

    private static final Logger log = LoggerFactory.getLogger(Module1And2Service.class);

    private final EndpointRepository endpointRepository;
    private final SecuritySystemRepository securitySystemRepository;
    private final ConnectionRepository connectionRepository;
    private final ObjectMapper objectMapper;

    public Module1And2Service(EndpointRepository endpointRepository,
                              SecuritySystemRepository securitySystemRepository,
                              ConnectionRepository connectionRepository,
                              ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.securitySystemRepository = securitySystemRepository;
        this.connectionRepository = connectionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Method 1: Returns list of all configured endpoints with their associated system and connection type.
     */
    @Transactional(readOnly = true)
    public List<EndpointDetailResponse> getAllEndpoints() {
        log.info("SAVIYNT MODULE 1 & 2: Fetching all configured endpoints with 3-tier hierarchy");
        List<EndpointEntity> endpoints = endpointRepository.findAll();

        return endpoints.stream().map(this::mapToDetailResponse).toList();
    }

    /**
     * Method 2: Fetches specific endpoint details by name.
     */
    @Transactional(readOnly = true)
    public EndpointDetailResponse getEndpointByName(String endpointName) {
        if (endpointName == null || endpointName.isBlank()) {
            throw new IllegalArgumentException("Endpoint name cannot be blank.");
        }

        log.info("SAVIYNT MODULE 1 & 2: Fetching endpoint config for endpointName='{}'", endpointName);
        EndpointEntity endpoint = endpointRepository.findByEndpointname(endpointName)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found with name: " + endpointName));

        return mapToDetailResponse(endpoint);
    }

    /**
     * Method 3: Executes live connectivity check to target system using connection configuration.
     * Reads connectionjson, extracts URL/host/port, and tests connectivity within 2-second timeout.
     */
    @Transactional(readOnly = true)
    public ConnectionTestResponse testConnection(Long connectionKey) {
        if (connectionKey == null || connectionKey <= 0) {
            throw new IllegalArgumentException("Connection key must be a positive integer.");
        }

        log.info("SAVIYNT MODULE 1 & 2: Testing connection for connectionKey={}", connectionKey);
        ConnectionEntity connection = connectionRepository.findById(connectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with key: " + connectionKey));

        String connectionJson = connection.getConnectionjson();
        if (connectionJson == null || connectionJson.isBlank()) {
            return new ConnectionTestResponse("FAILED", "Connection failed: connectionjson is empty or not configured.");
        }

        try {
            JsonNode root = objectMapper.readTree(connectionJson);
            String url = root.hasNonNull("url") ? root.get("url").asText() : null;
            String host = root.hasNonNull("host") ? root.get("host").asText() : null;
            int port = root.hasNonNull("port") ? root.get("port").asInt() : -1;

            if (url != null && !url.isBlank()) {
                URI uri = URI.create(url);
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    pingHttpUrl(uri.toURL());
                } else {
                    String targetHost = uri.getHost() != null ? uri.getHost() : host;
                    int targetPort = uri.getPort() != -1 ? uri.getPort() : (port != -1 ? port : ("https".equalsIgnoreCase(scheme) ? 443 : 80));
                    if (targetHost == null) {
                        return new ConnectionTestResponse("FAILED", "Connection failed: Invalid URL or missing host in configuration.");
                    }
                    pingSocket(targetHost, targetPort);
                }
            } else if (host != null && !host.isBlank()) {
                int targetPort = port > 0 ? port : 80;
                pingSocket(host, targetPort);
            } else {
                return new ConnectionTestResponse("FAILED", "Connection failed: Neither url nor host specified in connectionjson.");
            }

            return new ConnectionTestResponse("SUCCESS", "Connection established successfully to target.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid connection configuration: {}", e.getMessage());
            return new ConnectionTestResponse("FAILED", "Connection failed: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Target connectivity check failed: {}", e.getMessage());
            return new ConnectionTestResponse("FAILED", "Connection failed: " + e.getMessage());
        }
    }

    void pingHttpUrl(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.connect();
        conn.getResponseCode();
        conn.disconnect();
    }

    void pingSocket(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
        }
    }

    private EndpointDetailResponse mapToDetailResponse(EndpointEntity endpoint) {
        Long systemKey = endpoint.getSystemkey();
        Optional<SecuritySystemEntity> secSystemOpt = systemKey != null
                ? securitySystemRepository.findById(systemKey)
                : Optional.empty();

        String systemName = secSystemOpt.map(SecuritySystemEntity::getSystemname).orElse("N/A");
        Long connectionKey = secSystemOpt.map(SecuritySystemEntity::getConnectionkey).orElse(null);

        Optional<ConnectionEntity> connOpt = connectionKey != null
                ? connectionRepository.findById(connectionKey)
                : Optional.empty();

        String connectionName = connOpt.map(ConnectionEntity::getConnectionname).orElse("N/A");
        String connectionType = connOpt.map(ConnectionEntity::getConnectiontype).orElse("N/A");

        return new EndpointDetailResponse(
                endpoint.getEndpointkey(),
                endpoint.getEndpointname(),
                systemKey,
                systemName,
                connectionKey,
                connectionName,
                connectionType,
                endpoint.getAccountnamerule(),
                endpoint.getCorrelationrule(),
                endpoint.getStatus()
        );
    }
}
