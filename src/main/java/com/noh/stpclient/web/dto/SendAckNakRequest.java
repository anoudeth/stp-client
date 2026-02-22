package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Gateway send ACK/NAK request.
 *
 * @param messageId The message ID to acknowledge.
 * @param isAck True for ACK, false for NAK.
 */
public record SendAckNakRequest(
        @NotBlank(message = "Message ID cannot be blank")
        String messageId,
        @NotNull(message = "isAck cannot be null")
        Boolean isAck
) {}
