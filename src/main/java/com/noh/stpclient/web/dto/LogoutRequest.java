package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Gateway logout request.
 *
 * @param sessionId The session ID for authentication.
 */
public record LogoutRequest(
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId
) {}
