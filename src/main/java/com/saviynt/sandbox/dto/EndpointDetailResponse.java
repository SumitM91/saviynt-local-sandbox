package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed configuration of an endpoint, security system, and connection")
public record EndpointDetailResponse(
        @Schema(description = "Endpoint Primary Key", example = "10")
        Long endpointKey,

        @Schema(description = "Endpoint Name", example = "AWS_Production")
        String endpointName,

        @Schema(description = "Security System Primary Key", example = "1")
        Long systemKey,

        @Schema(description = "Security System Name", example = "AWS_SecSystem")
        String systemName,

        @Schema(description = "Connection Primary Key", example = "1")
        Long connectionKey,

        @Schema(description = "Connection Name", example = "AWS_REST_Conn")
        String connectionName,

        @Schema(description = "Connection Type (REST, AD, JDBC, SOAP)", example = "REST")
        String connectionType,

        @Schema(description = "Account Name Generation Rule", example = "${user.firstname}.${user.lastname}.aws")
        String accountNameRule,

        @Schema(description = "Identity Correlation Rule", example = "users.customproperty1 = accounts.customproperty1")
        String correlationRule,

        @Schema(description = "Status of the endpoint", example = "Active")
        String status
) {}
