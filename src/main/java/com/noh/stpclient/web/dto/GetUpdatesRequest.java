package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Gateway get updates request.
 *
 * @param sessionId The session ID for authentication.
 */
public record GetUpdatesRequest(
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId
) {}
