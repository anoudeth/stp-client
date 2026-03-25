package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Data;

/**
 * JAXB model for pacs.009.001.08 — Financial Institution Credit Transfer.
 * Document root element is FICdtTrf; Dbtr/Cdtr are financial institutions (FinInstnId/BICFI).
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataPDU", namespace = "urn:cma:stp:xsd:stp.1.0")
public class DataPDU009 {

    @XmlElement(name = "Body")
    private Body body;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Body {

        @XmlElement(name = "AppHdr", namespace = "urn:iso:std:iso:20022:tech:xsd:head.001.001.01")
        private AppHdr appHdr;

        @XmlElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08")
        private Document document;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AppHdr {

        @XmlElement(name = "Fr")
        private Fr fr;

        @XmlElement(name = "To")
        private To to;

        @XmlElement(name = "BizMsgIdr")
        private String bizMsgIdr;

        @XmlElement(name = "MsgDefIdr")
        private String msgDefIdr;

        @XmlElement(name = "BizSvc")
        private String bizSvc;

        @XmlElement(name = "CreDt")
        private String creDt;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Fr {

        @XmlElement(name = "FIId")
        private FIId fiId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class To {

        @XmlElement(name = "FIId")
        private FIId fiId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FIId {

        @XmlElement(name = "FinInstnId")
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinInstnId {

        @XmlElement(name = "BICFI")
        private String bicfi;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Document {

        @XmlElement(name = "FICdtTrf")
        private FICdtTrf fiCdtTrf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FICdtTrf {

        @XmlElement(name = "GrpHdr")
        private GrpHdr grpHdr;

        @XmlElement(name = "CdtTrfTxInf")
        private CdtTrfTxInf cdtTrfTxInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GrpHdr {

        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;

        @XmlElement(name = "NbOfTxs")
        private String nbOfTxs;

        @XmlElement(name = "SttlmInf")
        private SttlmInf sttlmInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SttlmInf {

        @XmlElement(name = "SttlmMtd")
        private String sttlmMtd;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtTrfTxInf {

        @XmlElement(name = "PmtId")
        private PmtId pmtId;

        @XmlElement(name = "PmtTpInf")
        private PmtTpInf pmtTpInf;

        @XmlElement(name = "IntrBkSttlmAmt")
        private IntrBkSttlmAmt intrBkSttlmAmt;

        @XmlElement(name = "IntrBkSttlmDt")
        private String intrBkSttlmDt;

        @XmlElement(name = "InstgAgt")
        private InstgAgt instgAgt;

        @XmlElement(name = "InstdAgt")
        private InstdAgt instdAgt;

        @XmlElement(name = "Dbtr")
        private Dbtr dbtr;

        @XmlElement(name = "DbtrAcct")
        private DbtrAcct dbtrAcct;

        @XmlElement(name = "Cdtr")
        private Cdtr cdtr;

        @XmlElement(name = "CdtrAcct")
        private CdtrAcct cdtrAcct;

        @XmlElement(name = "InstrForNxtAgt")
        private InstrForNxtAgt instrForNxtAgt;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PmtId {

        @XmlElement(name = "InstrId")
        private String instrId;

        @XmlElement(name = "EndToEndId")
        private String endToEndId;

        @XmlElement(name = "TxId")
        private String txId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PmtTpInf {

        @XmlElement(name = "ClrChanl")
        private String clrChanl;

        @XmlElement(name = "SvcLvl")
        private SvcLvl svcLvl;

        @XmlElement(name = "LclInstrm")
        private LclInstrm lclInstrm;

        @XmlElement(name = "CtgyPurp")
        private CtgyPurp ctgyPurp;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SvcLvl {

        @XmlElement(name = "Prtry")
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LclInstrm {

        @XmlElement(name = "Prtry")
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CtgyPurp {

        @XmlElement(name = "Prtry")
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IntrBkSttlmAmt {

        @XmlValue
        private String value;

        @XmlAttribute(name = "Ccy")
        private String ccy;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstgAgt {

        @XmlElement(name = "FinInstnId")
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstdAgt {

        @XmlElement(name = "FinInstnId")
        private FinInstnId finInstnId;
    }

    /** Financial institution debtor — identified by BICFI only */
    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Dbtr {

        @XmlElement(name = "FinInstnId")
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DbtrAcct {

        @XmlElement(name = "Id")
        private Id id;
    }

    /** Financial institution creditor — identified by BICFI only */
    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Cdtr {

        @XmlElement(name = "FinInstnId")
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtrAcct {

        @XmlElement(name = "Id")
        private Id id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstrForNxtAgt {

        @XmlElement(name = "InstrInf")
        private String instrInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Id {

        @XmlElement(name = "Othr")
        private Othr othr;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Othr {

        @XmlElement(name = "Id")
        private String id;
    }
}