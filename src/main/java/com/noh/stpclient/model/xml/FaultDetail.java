package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fault", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class FaultDetail {

    @XmlElement(required = true)
    protected String code;

    @XmlElement(required = true)
    protected String description;

    @XmlElement(required = true)
    protected String info;
}
