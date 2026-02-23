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
        "username",
        "password",
        "signature"
})
@XmlRootElement(name = "logon", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class Logon {

    @XmlElement(required = true)
    protected String username;

    @XmlElement(required = true)
    protected String password;

    @XmlElement(required = true)
    protected String signature;

}
