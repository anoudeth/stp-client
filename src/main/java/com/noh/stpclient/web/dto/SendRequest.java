package com.noh.stpclient.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlType;

/**
 * DTO for Gateway send request.
 *
 * @param sessionId The session ID for authentication.
 * @param message The message content to send.
 */
public record SendRequest(
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId,
        @NotNull(message = "Message content cannot be null")
        @Valid
        MessageContent message
) {
    /**
     * DTO for the message content.
     */
    public record MessageContent(
            @NotBlank(message = "block4 cannot be blank")
            String block4,
            @NotBlank(message = "msgReceiver cannot be blank")
            String msgReceiver,
            @NotBlank(message = "msgSender cannot be blank")
            String msgSender,
            @NotBlank(message = "msgSequence cannot be blank")
            String msgSequence,
            @NotBlank(message = "msgType cannot be blank")
            String msgType,
            @NotBlank(message = "format cannot be blank")
            String format
    ) {}
}
