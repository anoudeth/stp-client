package com.noh.stpclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noh.stpclient.model.xml.SendResponseData;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gateway response after submitting a financial transaction")
public record SendResponseDto(
        @Schema(description = "Response type from gateway", example = "ACK")
        String type,
        @Schema(description = "Gateway response datetime", example = "2603141030")
        String datetime,
        @Schema(description = "Message Input Reference assigned by the gateway", example = "260314LBBCLALABXXX0001000042")
        String mir,
        @Schema(description = "Transaction reference number", example = "REF-2026-0042")
        String ref,
        @Schema(description = "Gateway result code", example = "000")
        String code,
        @Schema(description = "Human-readable description of the gateway result", example = "Message accepted")
        String description,
        @Schema(description = "Additional information from the gateway")
        String info
) {
    public static SendResponseDto from(SendResponseData data) {
        return new SendResponseDto(
                data.getType(),
                data.getDatetime(),
                data.getMir(),
                data.getRef(),
                data.getCode(),
                data.getDescription(),
                data.getInfo()
        );
    }
}
