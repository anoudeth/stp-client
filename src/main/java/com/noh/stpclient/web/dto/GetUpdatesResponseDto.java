package com.noh.stpclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noh.stpclient.model.xml.GetUpdatesItem;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetUpdatesResponseDto(
        String block4,
        String msgCopySrvId,
        String msgCopySrvInfo,
        String msgDelNotifRq,
        String msgFinValidation,
        String msgFormat,
        String msgNetInputTime,
        String msgNetMir,
        String msgNetOutputDate,
        String msgPacResult,
        String msgPde,
        String msgPdm,
        String msgPriority,
        String msgReceiver,
        String msgSender,
        String msgSequence,
        String msgSession,
        String msgSubFormat,
        String msgType,
        String msgUserPriority,
        String msgUserReference,
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
