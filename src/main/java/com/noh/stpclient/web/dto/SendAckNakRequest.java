package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for Gateway sendACKNAK request.
 * Contains the session ID from the getUpdates call and the data fields from the received message.
 *
 * @param sessionId The session ID used for the getUpdates call that returned this message.
 * @param type      "ACK" to acknowledge, "NAK" to reject.
 * @param datetime  The datetime field from the received message (e.g. "2603041641").
 * @param mir       The MIR from the received message (e.g. "260304LBBCLALABXXX0001000007").
 */
public record SendAckNakRequest(
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId,

        @NotBlank(message = "Type cannot be blank")
        @Pattern(regexp = "ACK|NAK", message = "Type must be ACK or NAK")
        String type,

        @NotBlank(message = "Datetime cannot be blank")
        String datetime,

        @NotBlank(message = "MIR cannot be blank")
        String mir
) {}
