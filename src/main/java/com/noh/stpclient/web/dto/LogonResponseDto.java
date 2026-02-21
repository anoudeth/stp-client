package com.noh.stpclient.web.dto;

/**
 * DTO for Gateway logon response.
 *
 * @param sessionId The session ID received from the gateway.
 */
public record LogonResponseDto(String sessionId) {}
