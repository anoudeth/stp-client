package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "getUpdatesResponse", namespace = "http://integration.gwclient.smallsystems.cma.se/")
public class GetUpdatesResponse {

    @XmlElement(name = "item")
    private List<GetUpdatesItem> items;
}
