package com.noh.stpclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noh.stpclient.model.xml.GetUpdatesItem;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A single inbound SWIFT message retrieved from the gateway")
public record GetUpdatesResponseDto(
        @Schema(description = "SWIFT message Block 4 (text block containing the actual message body)")
        String block4,
        @Schema(description = "Copy service identifier", example = "S.W.I.F.T.")
        String msgCopySrvId,
        @Schema(description = "Copy service additional information")
        String msgCopySrvInfo,
        @Schema(description = "Delivery notification request flag")
        String msgDelNotifRq,
        @Schema(description = "Financial validation result")
        String msgFinValidation,
        @Schema(description = "SWIFT message format", example = "FIN")
        String msgFormat,
        @Schema(description = "Network input timestamp", example = "1030")
        String msgNetInputTime,
        @Schema(description = "Message Input Reference (MIR)", example = "260314LBBCLALABXXX0001000007")
        String msgNetMir,
        @Schema(description = "Network output date", example = "260314")
        String msgNetOutputDate,
        @Schema(description = "PAC (Possible Alternate Coding) validation result")
        String msgPacResult,
        @Schema(description = "Possible Duplicate Emission indicator")
        String msgPde,
        @Schema(description = "Possible Duplicate Message indicator")
        String msgPdm,
        @Schema(description = "Message priority", example = "N")
        String msgPriority,
        @Schema(description = "BIC of the message receiver", example = "LBBCLALABXXX")
        String msgReceiver,
        @Schema(description = "BIC of the message sender", example = "LPDRLALAXATS")
        String msgSender,
        @Schema(description = "SWIFT message sequence number", example = "0000007")
        String msgSequence,
        @Schema(description = "SWIFT session number", example = "0001")
        String msgSession,
        @Schema(description = "Sub-format of the message")
        String msgSubFormat,
        @Schema(description = "SWIFT message type", example = "103")
        String msgType,
        @Schema(description = "User-defined message priority")
        String msgUserPriority,
        @Schema(description = "User-defined message reference")
        String msgUserReference,
        @Schema(description = "Overall message format descriptor", example = "FIN")
        String format
) {
    public static GetUpdatesResponseDto from(GetUpdatesItem item) {
        return new GetUpdatesResponseDto(
                item.getBlock4(),
                item.getMsgCopySrvId(),
                item.getMsgCopySrvInfo(),
                item.getMsgDelNotifRq(),
                item.getMsgFinValidation(),
                item.getMsgFormat(),
                item.getMsgNetInputTime(),
                item.getMsgNetMir(),
                item.getMsgNetOutputDate(),
                item.getMsgPacResult(),
                item.getMsgPde(),
                item.getMsgPdm(),
                item.getMsgPriority(),
                item.getMsgReceiver(),
                item.getMsgSender(),
                item.getMsgSequence(),
                item.getMsgSession(),
                item.getMsgSubFormat(),
                item.getMsgType(),
                item.getMsgUserPriority(),
                item.getMsgUserReference(),
                item.getFormat()
        );
    }
}
