package com.noh.stpclient.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataPDU", namespace = "urn:cma:stp:xsd:stp.1.0")
public class DataPDU {

    @XmlElement(name = "Body")
    private Body body;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Body {
        
        @XmlElement(name = "AppHdr", namespace = "urn:iso:std:iso:20022:tech:xsd:head.001.001.01")
        private AppHdr appHdr;

        @XmlElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08")
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

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinInstnId {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "BICFI", namespace = NS)
        private String bicfi;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08")
    public static class Document {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FIToFICstmrCdtTrf", namespace = NS)
        private FIToFICstmrCdtTrf fiToFICstmrCdtTrf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FIToFICstmrCdtTrf {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "GrpHdr", namespace = NS)
        private GrpHdr grpHdr;

        @XmlElement(name = "CdtTrfTxInf", namespace = NS)
        private CdtTrfTxInf cdtTrfTxInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GrpHdr {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "MsgId", namespace = NS)
        private String msgId;

        @XmlElement(name = "CreDtTm", namespace = NS)
        private String creDtTm;

        @XmlElement(name = "NbOfTxs", namespace = NS)
        private String nbOfTxs;

        @XmlElement(name = "SttlmInf", namespace = NS)
        private SttlmInf sttlmInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SttlmInf {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "SttlmMtd", namespace = NS)
        private String sttlmMtd;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtTrfTxInf {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "PmtId", namespace = NS)
        private PmtId pmtId;

        @XmlElement(name = "PmtTpInf", namespace = NS)
        private PmtTpInf pmtTpInf;

        @XmlElement(name = "IntrBkSttlmAmt", namespace = NS)
        private IntrBkSttlmAmt intrBkSttlmAmt;

        @XmlElement(name = "IntrBkSttlmDt", namespace = NS)
        private String intrBkSttlmDt;

        @XmlElement(name = "ChrgBr", namespace = NS)
        private String chrgBr;

        @XmlElement(name = "InstgAgt", namespace = NS)
        private InstgAgt instgAgt;

        @XmlElement(name = "InstdAgt", namespace = NS)
        private InstdAgt instdAgt;

        @XmlElement(name = "Dbtr", namespace = NS)
        private Dbtr dbtr;

        @XmlElement(name = "DbtrAcct", namespace = NS)
        private DbtrAcct dbtrAcct;

        @XmlElement(name = "DbtrAgt", namespace = NS)
        private DbtrAgt dbtrAgt;

        @XmlElement(name = "DbtrAgtAcct", namespace = NS)
        private DbtrAgtAcct dbtrAgtAcct;

        @XmlElement(name = "CdtrAgt", namespace = NS)
        private CdtrAgt cdtrAgt;

        @XmlElement(name = "CdtrAgtAcct", namespace = NS)
        private CdtrAgtAcct cdtrAgtAcct;

        @XmlElement(name = "Cdtr", namespace = NS)
        private Cdtr cdtr;

        @XmlElement(name = "CdtrAcct", namespace = NS)
        private CdtrAcct cdtrAcct;

        @XmlElement(name = "InstrForNxtAgt", namespace = NS)
        private InstrForNxtAgt instrForNxtAgt;

        @XmlElement(name = "RmtInf", namespace = NS)
        private RmtInf rmtInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PmtId {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "InstrId", namespace = NS)
        private String instrId;

        @XmlElement(name = "EndToEndId", namespace = NS)
        private String endToEndId;

        @XmlElement(name = "TxId", namespace = NS)
        private String txId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PmtTpInf {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "ClrChanl", namespace = NS)
        private String clrChanl;

        @XmlElement(name = "SvcLvl", namespace = NS)
        private SvcLvl svcLvl;

        @XmlElement(name = "LclInstrm", namespace = NS)
        private LclInstrm lclInstrm;

        @XmlElement(name = "CtgyPurp", namespace = NS)
        private CtgyPurp ctgyPurp;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SvcLvl {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Prtry", namespace = NS)
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LclInstrm {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Prtry", namespace = NS)
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CtgyPurp {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Prtry", namespace = NS)
        private String prtry;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IntrBkSttlmAmt {

        private String value;

        @XmlAttribute(name = "Ccy")
        private String ccy;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstgAgt {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstdAgt {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Dbtr {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Nm", namespace = NS)
        private String nm;

        @XmlElement(name = "PstlAdr", namespace = NS)
        private PstlAdr pstlAdr;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PstlAdr {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "AdrLine", namespace = NS)
        private List<String> adrLine;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DbtrAcct {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Id", namespace = NS)
        private Id id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DbtrAgt {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DbtrAgtAcct {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Id", namespace = NS)
        private Id id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtrAgt {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinInstnId finInstnId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtrAgtAcct {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Id", namespace = NS)
        private Id id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Cdtr {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Nm", namespace = NS)
        private String nm;

        @XmlElement(name = "PstlAdr", namespace = NS)
        private PstlAdr pstlAdr;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstrForNxtAgt {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "InstrInf", namespace = NS)
        private String instrInf;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CdtrAcct {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Id", namespace = NS)
        private Id id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Id {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Othr", namespace = NS)
        private Othr othr;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Othr {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Id", namespace = NS)
        private String id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RmtInf {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        @XmlElement(name = "Ustrd", namespace = NS)
        private String ustrd;
    }
}
