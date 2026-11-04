package com.saviynt.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of target connection verification test")
public record ConnectionTestResponse(
        @Schema(description = "Status of connection verification (SUCCESS or FAILED)", example = "SUCCESS")
        String status,

        @Schema(description = "Descriptive outcome message", example = "Connection established successfully to target.")
        String message
) {}
