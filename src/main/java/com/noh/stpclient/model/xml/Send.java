package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "send", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class Send {

    @XmlElement(name = "session_id", required = true)
    private String sessionId;

    @XmlElement(required = true)
    private Message message;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "block4",
            "msgReceiver",
            "msgSender",
            "msgSequence",
            "msgType",
            "format"
    })
    public static class Message {
        @XmlElement(required = true)
        private String block4;

        @XmlElement(required = true)
        private String msgReceiver;

        @XmlElement(required = true)
        private String msgSender;

        @XmlElement(required = true)
        private String msgType;

        @XmlElement(required = true)
        private String msgSequence;

        @XmlElement(required = true)
        private String format;
    }
}
