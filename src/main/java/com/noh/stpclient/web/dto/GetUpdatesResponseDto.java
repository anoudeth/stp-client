package com.noh.stpclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noh.stpclient.model.xml.GetUpdatesResponse.ParamsMtMsg;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetUpdatesResponseDto(
        Long   msgId,
        String msgType,
        String msgSender,
        String msgReceiver,
        String msgFormat,
        String msgSubFormat,
        String format,
        String msgSession,
        String msgSequence,
        String msgPriority,
        String msgUserPriority,
        String msgUserReference,
        String refMsgUserReference,
        String msgNetMir,
        String msgNetInputTime,
        String msgNetOutputDate,
        String msgMacResult,
        String msgPacResult,
        String msgFinValidation,
        String msgPde,
        String msgPdm,
        String msgCopySrvId,
        String msgCopySrvInfo,
        String msgDelNotifRq,
        String block4
) {
    public static GetUpdatesResponseDto from(ParamsMtMsg msg) {
        return new GetUpdatesResponseDto(
                msg.getMsgId(),
                msg.getMsgType(),
                msg.getMsgSender(),
                msg.getMsgReceiver(),
                msg.getMsgFormat(),
                msg.getMsgSubFormat(),
                msg.getFormat(),
                msg.getMsgSession(),
                msg.getMsgSequence(),
                msg.getMsgPriority(),
                msg.getMsgUserPriority(),
                msg.getMsgUserReference(),
                msg.getRefMsgUserReference(),
                msg.getMsgNetMir(),
                msg.getMsgNetInputTime(),
                msg.getMsgNetOutputDate(),
                msg.getMsgMacResult(),
                msg.getMsgPacResult(),
                msg.getMsgFinValidation(),
                msg.getMsgPde(),
                msg.getMsgPdm(),
                msg.getMsgCopySrvId(),
                msg.getMsgCopySrvInfo(),
                msg.getMsgDelNotifRq(),
                msg.getBlock4()
        );
    }
}
