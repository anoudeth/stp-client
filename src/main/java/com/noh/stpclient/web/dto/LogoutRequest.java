package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Gateway logout request.
 *
 * @param sessionId The session ID for authentication.
 */
@Schema(description = "Session termination request")
public record LogoutRequest(
        @NotBlank(message = "Session ID cannot be blank")
        @Schema(description = "Active gateway session ID to terminate", example = "SES-20260314-001")
        String sessionId
) {}
