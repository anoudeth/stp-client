package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for Gateway sendACKNAK request.
 * Contains the data fields from the received message. Session ID is managed internally.
 *
 * @param type      "ACK" to acknowledge, "NAK" to reject.
 * @param datetime  The datetime field from the received message (e.g. "2603041641").
 * @param mir       The MIR from the received message (e.g. "260304LBBCLALABXXX0001000007").
 */
@Schema(description = "ACK or NAK for a received SWIFT message")
public record SendAckNakRequest(
        @NotBlank(message = "Type cannot be blank")
        @Pattern(regexp = "ACK|NAK", message = "Type must be ACK or NAK")
        @Schema(description = "Acknowledgment type", allowableValues = {"ACK", "NAK"}, example = "ACK")
        String type,

        @NotBlank(message = "Datetime cannot be blank")
        @Schema(description = "Datetime field from the received message", example = "2603041641")
        String datetime,

        @NotBlank(message = "MIR cannot be blank")
        @Schema(description = "Message Input Reference from the received message", example = "260304LBBCLALABXXX0001000007")
        String mir
) {}
