package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "sendAckNak", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class SendAckNak {

    @XmlElement(name = "message_id", required = true)
    private String messageId;

    @XmlElement(name = "is_ack", required = true)
    private boolean isAck;
}
