package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "sendACKNAK", namespace = "http://integration.gwclient.smallsystems.cma.se/")
@XmlType(name = "", propOrder = {"sessionId", "data"})
public class SendAckNak {

    @XmlElement(name = "session_id", required = true)
    private String sessionId;

    @XmlElement(required = true)
    private SendResponseData data;
}
