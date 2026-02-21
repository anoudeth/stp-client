package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "sessionId"
})
@XmlRootElement(name = "logonResponse", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class LogonResponse {

    @XmlElement(name = "session_id", required = true)
    protected String sessionId;

    // Getters and Setters
//
//    public String getSessionId() {
//        return sessionId;
//    }
//
//    public void setSessionId(String value) {
//        this.sessionId = value;
//    }
}