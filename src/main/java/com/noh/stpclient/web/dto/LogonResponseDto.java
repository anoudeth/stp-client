package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for Gateway logon response.
 *
 * @param sessionId The session ID received from the gateway.
 */
@Schema(description = "Gateway logon response containing the session ID")
public record LogonResponseDto(
        @Schema(description = "Active gateway session ID to use in subsequent requests", example = "SES-20260314-001")
        String sessionId
) {}
