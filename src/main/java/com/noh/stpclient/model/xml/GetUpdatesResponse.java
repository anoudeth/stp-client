package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "getUpdatesResponse", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class GetUpdatesResponse {

    @XmlElement(name = "item")
    private List<ParamsMtMsg> items = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "ParamsMtMsg", namespace = "http://integration.gwclient.smallsystems.cma.se/",
            propOrder = {
                    "block4",
                    "msgCopySrvId",
                    "msgCopySrvInfo",
                    "msgDelNotifRq",
                    "msgFinValidation",
                    "msgFormat",
                    "msgId",
                    "msgMacResult",
                    "msgNetInputTime",
                    "msgNetMir",
                    "msgNetOutputDate",
                    "msgPacResult",
                    "msgPde",
                    "msgPdm",
                    "msgPriority",
                    "msgReceiver",
                    "msgSender",
                    "msgSequence",
                    "msgSession",
                    "msgSubFormat",
                    "msgType",
                    "msgUserPriority",
                    "msgUserReference",
                    "format",
                    "refMsgUserReference"
            })
    public static class ParamsMtMsg {

        @XmlElement(required = true)
        private String block4;

        private String msgCopySrvId;
        private String msgCopySrvInfo;
        private String msgDelNotifRq;
        private String msgFinValidation;
        private String msgFormat;
        private Long msgId;
        private String msgMacResult;

        /** Timestamp from the gateway network — maps to {@code datetime} in sendACKNAK */
        private String msgNetInputTime;

        /** MIR from the gateway network — maps to {@code mir} in sendACKNAK */
        private String msgNetMir;

        private String msgNetOutputDate;
        private String msgPacResult;
        private String msgPde;
        private String msgPdm;
        private String msgPriority;

        @XmlElement(required = true)
        private String msgReceiver;

        @XmlElement(required = true)
        private String msgSender;

        private String msgSequence;
        private String msgSession;
        private String msgSubFormat;

        @XmlElement(required = true)
        private String msgType;

        private String msgUserPriority;
        private String msgUserReference;

        @XmlElement(required = true)
        private String format;

        private String refMsgUserReference;
    }
}
