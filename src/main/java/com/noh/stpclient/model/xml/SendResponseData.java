package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "data", propOrder = {
        "type",
        "datetime",
        "mir",
        "ref",
        "code",
        "description",
        "info"
})
public class SendResponseData {

    @XmlElement(required = true)
    protected String type;
    @XmlElement(required = true)
    protected String datetime;
    @XmlElement(required = true)
    protected String mir;
    @XmlElement(required = true)
    protected String ref;
    protected String code;
    protected String description;
    protected String info;

}
