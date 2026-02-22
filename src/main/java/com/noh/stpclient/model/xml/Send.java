package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
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
        private String msgUserReference;

        @XmlElement(required = true)
        private String format;
    }
}
