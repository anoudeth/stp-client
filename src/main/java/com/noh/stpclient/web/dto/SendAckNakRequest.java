package com.noh.stpclient.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for Gateway sendACKNAK request.
 * Contains the data fields from the received message to be acknowledged.
 *
 * @param type     "ACK" to acknowledge, "NAK" to reject.
 * @param datetime The datetime field from the received message (e.g. "2603041641").
 * @param mir      The MIR from the received message (e.g. "260304LBBCLALABXXX0001000007").
 */
public record SendAckNakRequest(
        @NotBlank(message = "Type cannot be blank")
        @Pattern(regexp = "ACK|NAK", message = "Type must be ACK or NAK")
        String type,

        @NotBlank(message = "Datetime cannot be blank")
        String datetime,

        @NotBlank(message = "MIR cannot be blank")
        String mir
) {}
