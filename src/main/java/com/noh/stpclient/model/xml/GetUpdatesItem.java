package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item")
public class GetUpdatesItem {

    @XmlElement
    private String block4;
    @XmlElement
    private String msgCopySrvId;
    @XmlElement
    private String msgCopySrvInfo;
    @XmlElement
    private String msgDelNotifRq;
    @XmlElement
    private String msgFinValidation;
    @XmlElement
    private String msgFormat;
    @XmlElement
    private String msgNetInputTime;
    @XmlElement
    private String msgNetMir;
    @XmlElement
    private String msgNetOutputDate;
    @XmlElement
    private String msgPacResult;
    @XmlElement
    private String msgPde;
    @XmlElement
    private String msgPdm;
    @XmlElement
    private String msgPriority;
    @XmlElement
    private String msgReceiver;
    @XmlElement
    private String msgSender;
    @XmlElement
    private String msgSequence;
    @XmlElement
    private String msgSession;
    @XmlElement
    private String msgSubFormat;
    @XmlElement
    private String msgType;
    @XmlElement
    private String msgUserPriority;
    @XmlElement
    private String msgUserReference;
    @XmlElement
    private String format;
}
