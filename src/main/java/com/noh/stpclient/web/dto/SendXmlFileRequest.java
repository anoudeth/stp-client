package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to sign a saved raw XML file and send it to the gateway")
public record SendXmlFileRequest(

        @NotBlank(message = "filename cannot be blank")
        @Schema(description = "Filename inside the xmlfile/ directory (e.g. 'msg123_raw.xml')", example = "msg123_raw.xml")
        String filename,

        @NotBlank(message = "msgType cannot be blank")
        @Schema(description = "SWIFT message type (e.g. pacs.009.001.03)", example = "pacs.009.001.03")
        String msgType,

        @Schema(description = "Message sequence number", example = "0000000001")
        String msgSequence
) {}
