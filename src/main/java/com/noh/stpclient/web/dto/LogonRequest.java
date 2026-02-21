package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Gateway logon request.
 *
 * @param username The username for authentication.
 * @param password The password for authentication.
 */
public record LogonRequest(
        @NotBlank(message = "Username cannot be blank")
        String username,
        @NotBlank(message = "Password cannot be blank")
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
