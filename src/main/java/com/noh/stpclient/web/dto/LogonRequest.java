package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Gateway logon request.
 *
 * @param username The username for authentication.
 * @param password The password for authentication.
 */
@Schema(description = "Credentials for gateway authentication")
public record LogonRequest(
        @NotBlank(message = "Username cannot be blank")
        @Schema(description = "Gateway username", example = "LBBCLALABXXX")
        String username,
        @NotBlank(message = "Password cannot be blank")
        @Schema(description = "Gateway password", example = "••••••••")
        String password
) {
    @Override
    public String toString() {
        return """
               {
                 "username": "%s",
                 "password": "******"
               }
               """.formatted(username, password);
    }
}
