package com.noh.stpclient.service;

import com.noh.stpclient.model.xml.DataPDU;
import com.noh.stpclient.model.xml.DataPDU009;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms financial transaction requests to DataPDU XML.
 * Dispatches to pacs.008 or pacs.009 builder based on msgType.
 */
@Service
@Slf4j
public class DataPDUTransformer {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds the DataPDU object for the given request.
     * Returns DataPDU (pacs.008) or DataPDU009 (pacs.009) based on msgType.
     */
    public Object transformToDataPDU(FinancialTransactionRequest request) {
        String msgType = request.transaction().msgType();
        if (msgType != null && msgType.startsWith("pacs.009")) {
            log.debug("Building DataPDU009 for msgType={}", msgType);
            return buildPacs009(request);
        }
        log.debug("Building DataPDU008 for msgType={}", msgType);
        return buildPacs008(request);
    }

    /**
     * Marshals a DataPDU (pacs.008 or pacs.009) object to XML string.
     * The Document xmlns is derived from msgType at runtime.
     */
    public String marshalToXml(Object dataPDU, String msgType) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(dataPDU.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

        StringWriter writer = new StringWriter();
        marshaller.marshal(dataPDU, writer);

        String xml = normalizeNamespacePrefixes(writer.toString(), msgType);
        log.debug("Generated XML: {}", xml);
        return xml;
    }

    // -------------------------------------------------------------------------
    // pacs.008 builder
    // -------------------------------------------------------------------------

    private DataPDU buildPacs008(FinancialTransactionRequest request) {
        var t = request.transaction();
        String now = ZonedDateTime.now().format(ISO_DATE_TIME);

        DataPDU dataPDU = new DataPDU();
        DataPDU.Body body = new DataPDU.Body();
        body.setAppHdr(buildAppHdr008(t, now));
        body.setDocument(buildDocument008(t, now));
        dataPDU.setBody(body);
        return dataPDU;
    }

    private DataPDU.AppHdr buildAppHdr008(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU.AppHdr appHdr = new DataPDU.AppHdr();

        DataPDU.Fr fr = new DataPDU.Fr();
        DataPDU.FIId frFiId = new DataPDU.FIId();
        DataPDU.FinInstnId frFi = new DataPDU.FinInstnId();
        frFi.setBicfi(t.senderBic());
        frFiId.setFinInstnId(frFi);
        fr.setFiId(frFiId);
        appHdr.setFr(fr);

        DataPDU.To to = new DataPDU.To();
        DataPDU.FIId toFiId = new DataPDU.FIId();
        DataPDU.FinInstnId toFi = new DataPDU.FinInstnId();
        toFi.setBicfi(t.receiverBic());
        toFiId.setFinInstnId(toFi);
        to.setFiId(toFiId);
        appHdr.setTo(to);

        appHdr.setBizMsgIdr(t.businessMessageId());
        appHdr.setMsgDefIdr(t.msgType());
        appHdr.setBizSvc(t.bizSvc());
        appHdr.setCreDt(now);
        return appHdr;
    }

    private DataPDU.Document buildDocument008(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU.Document document = new DataPDU.Document();
        DataPDU.FIToFICstmrCdtTrf root = new DataPDU.FIToFICstmrCdtTrf();
        root.setGrpHdr(buildGrpHdr008(t, now));
        root.setCdtTrfTxInf(buildCdtTrfTxInf008(t));
        document.setFiToFICstmrCdtTrf(root);
        return document;
    }

    private DataPDU.GrpHdr buildGrpHdr008(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU.GrpHdr grpHdr = new DataPDU.GrpHdr();
        grpHdr.setMsgId(t.messageId());
        grpHdr.setCreDtTm(now.substring(0, 19));
        grpHdr.setNbOfTxs("1");
        DataPDU.SttlmInf sttlmInf = new DataPDU.SttlmInf();
        sttlmInf.setSttlmMtd("CLRG");
        grpHdr.setSttlmInf(sttlmInf);
        return grpHdr;
    }

    private DataPDU.CdtTrfTxInf buildCdtTrfTxInf008(FinancialTransactionRequest.TransactionData t) {
        DataPDU.CdtTrfTxInf tx = new DataPDU.CdtTrfTxInf();

        DataPDU.PmtId pmtId = new DataPDU.PmtId();
        pmtId.setInstrId(t.messageId());
        pmtId.setEndToEndId("NOTPROVIDED");
        pmtId.setTxId(t.messageId());
        tx.setPmtId(pmtId);

        tx.setPmtTpInf(buildPmtTpInf008());

        DataPDU.IntrBkSttlmAmt amt = new DataPDU.IntrBkSttlmAmt();
        amt.setValue(t.amount().toString());
        amt.setCcy(t.currency());
        tx.setIntrBkSttlmAmt(amt);
        tx.setIntrBkSttlmDt(t.settlementDate());
        tx.setChrgBr("SHAR");

        DataPDU.InstgAgt instgAgt = new DataPDU.InstgAgt();
        DataPDU.FinInstnId instgFi = new DataPDU.FinInstnId();
        instgFi.setBicfi(t.instructingAgentBic());
        instgAgt.setFinInstnId(instgFi);
        tx.setInstgAgt(instgAgt);

        DataPDU.InstdAgt instdAgt = new DataPDU.InstdAgt();
        DataPDU.FinInstnId instdFi = new DataPDU.FinInstnId();
        instdFi.setBicfi(t.instructedAgentBic());
        instdAgt.setFinInstnId(instdFi);
        tx.setInstdAgt(instdAgt);

        DataPDU.Dbtr dbtr = new DataPDU.Dbtr();
        dbtr.setNm(t.debtorName());
        DataPDU.PstlAdr dbtrAdr = new DataPDU.PstlAdr();
        if (t.debtorAddressLines() != null && !t.debtorAddressLines().isEmpty()) {
            dbtrAdr.setAdrLine(t.debtorAddressLines());
        }
        dbtr.setPstlAdr(dbtrAdr);
        tx.setDbtr(dbtr);

        DataPDU.DbtrAcct dbtrAcct = new DataPDU.DbtrAcct();
        DataPDU.Id dbtrId = new DataPDU.Id();
        DataPDU.Othr dbtrOthr = new DataPDU.Othr();
        dbtrOthr.setId(t.debtorAccount());
        dbtrId.setOthr(dbtrOthr);
        dbtrAcct.setId(dbtrId);
        tx.setDbtrAcct(dbtrAcct);

        DataPDU.DbtrAgt dbtrAgt = new DataPDU.DbtrAgt();
        DataPDU.FinInstnId dbtrAgtFi = new DataPDU.FinInstnId();
        dbtrAgtFi.setBicfi(t.debtorAgentBic());
        dbtrAgt.setFinInstnId(dbtrAgtFi);
        tx.setDbtrAgt(dbtrAgt);

        DataPDU.DbtrAgtAcct dbtrAgtAcct = new DataPDU.DbtrAgtAcct();
        DataPDU.Id dbtrAgtId = new DataPDU.Id();
        DataPDU.Othr dbtrAgtOthr = new DataPDU.Othr();
        dbtrAgtOthr.setId(t.debtorAgentAccount());
        dbtrAgtId.setOthr(dbtrAgtOthr);
        dbtrAgtAcct.setId(dbtrAgtId);
        tx.setDbtrAgtAcct(dbtrAgtAcct);

        DataPDU.CdtrAgt cdtrAgt = new DataPDU.CdtrAgt();
        DataPDU.FinInstnId cdtrAgtFi = new DataPDU.FinInstnId();
        cdtrAgtFi.setBicfi(t.instructedAgentBic());
        cdtrAgt.setFinInstnId(cdtrAgtFi);
        tx.setCdtrAgt(cdtrAgt);

        DataPDU.CdtrAgtAcct cdtrAgtAcct = new DataPDU.CdtrAgtAcct();
        DataPDU.Id cdtrAgtId = new DataPDU.Id();
        DataPDU.Othr cdtrAgtOthr = new DataPDU.Othr();
        cdtrAgtOthr.setId(t.creditorAgentAccount());
        cdtrAgtId.setOthr(cdtrAgtOthr);
        cdtrAgtAcct.setId(cdtrAgtId);
        tx.setCdtrAgtAcct(cdtrAgtAcct);

        DataPDU.Cdtr cdtr = new DataPDU.Cdtr();
        cdtr.setNm(t.creditorName());
        DataPDU.PstlAdr cdtrAdr = new DataPDU.PstlAdr();
        if (t.creditorAddressLines() != null && !t.creditorAddressLines().isEmpty()) {
            cdtrAdr.setAdrLine(t.creditorAddressLines());
        }
        cdtr.setPstlAdr(cdtrAdr);
        tx.setCdtr(cdtr);

        DataPDU.CdtrAcct cdtrAcct = new DataPDU.CdtrAcct();
        DataPDU.Id cdtrId = new DataPDU.Id();
        DataPDU.Othr cdtrOthr = new DataPDU.Othr();
        cdtrOthr.setId(t.creditorAccount());
        cdtrId.setOthr(cdtrOthr);
        cdtrAcct.setId(cdtrId);
        tx.setCdtrAcct(cdtrAcct);

        if (t.instrForNxtAgt() != null) {
            DataPDU.InstrForNxtAgt instr = new DataPDU.InstrForNxtAgt();
            instr.setInstrInf(t.instrForNxtAgt());
            tx.setInstrForNxtAgt(instr);
        }

        DataPDU.RmtInf rmtInf = new DataPDU.RmtInf();
        rmtInf.setUstrd(t.remittanceInformation());
        tx.setRmtInf(rmtInf);

        return tx;
    }

    private DataPDU.PmtTpInf buildPmtTpInf008() {
        DataPDU.PmtTpInf pmtTpInf = new DataPDU.PmtTpInf();
        pmtTpInf.setClrChanl("RTGS");
        DataPDU.SvcLvl svcLvl = new DataPDU.SvcLvl();
        svcLvl.setPrtry("0010");
        pmtTpInf.setSvcLvl(svcLvl);
        DataPDU.LclInstrm lclInstrm = new DataPDU.LclInstrm();
        lclInstrm.setPrtry("RTGS-SSCT");
        pmtTpInf.setLclInstrm(lclInstrm);
        DataPDU.CtgyPurp ctgyPurp = new DataPDU.CtgyPurp();
        ctgyPurp.setPrtry("001");
        pmtTpInf.setCtgyPurp(ctgyPurp);
        return pmtTpInf;
    }

    // -------------------------------------------------------------------------
    // pacs.009 builder
    // -------------------------------------------------------------------------

    private DataPDU009 buildPacs009(FinancialTransactionRequest request) {
        var t = request.transaction();
        String now = ZonedDateTime.now().format(ISO_DATE_TIME);

        DataPDU009 dataPDU = new DataPDU009();
        DataPDU009.Body body = new DataPDU009.Body();
        body.setAppHdr(buildAppHdr009(t, now));
        body.setDocument(buildDocument009(t, now));
        dataPDU.setBody(body);
        return dataPDU;
    }

    private DataPDU009.AppHdr buildAppHdr009(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU009.AppHdr appHdr = new DataPDU009.AppHdr();

        DataPDU009.Fr fr = new DataPDU009.Fr();
        DataPDU009.FIId frFiId = new DataPDU009.FIId();
        DataPDU009.FinInstnId frFi = new DataPDU009.FinInstnId();
        frFi.setBicfi(t.senderBic());
        frFiId.setFinInstnId(frFi);
        fr.setFiId(frFiId);
        appHdr.setFr(fr);

        DataPDU009.To to = new DataPDU009.To();
        DataPDU009.FIId toFiId = new DataPDU009.FIId();
        DataPDU009.FinInstnId toFi = new DataPDU009.FinInstnId();
        toFi.setBicfi(t.receiverBic());
        toFiId.setFinInstnId(toFi);
        to.setFiId(toFiId);
        appHdr.setTo(to);

        appHdr.setBizMsgIdr(t.businessMessageId());
        appHdr.setMsgDefIdr(t.msgType());
        appHdr.setBizSvc(t.bizSvc());
        appHdr.setCreDt(now);
        return appHdr;
    }

    private DataPDU009.Document buildDocument009(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU009.Document document = new DataPDU009.Document();
        DataPDU009.FICdtTrf root = new DataPDU009.FICdtTrf();
        root.setGrpHdr(buildGrpHdr009(t, now));
        root.setCdtTrfTxInf(buildCdtTrfTxInf009(t));
        document.setFiCdtTrf(root);
        return document;
    }

    private DataPDU009.GrpHdr buildGrpHdr009(FinancialTransactionRequest.TransactionData t, String now) {
        DataPDU009.GrpHdr grpHdr = new DataPDU009.GrpHdr();
        grpHdr.setMsgId(t.messageId());
        grpHdr.setCreDtTm(now.substring(0, 19));
        grpHdr.setNbOfTxs("1");
        DataPDU009.SttlmInf sttlmInf = new DataPDU009.SttlmInf();
        sttlmInf.setSttlmMtd("CLRG");
        grpHdr.setSttlmInf(sttlmInf);
        return grpHdr;
    }

    private DataPDU009.CdtTrfTxInf buildCdtTrfTxInf009(FinancialTransactionRequest.TransactionData t) {
        DataPDU009.CdtTrfTxInf tx = new DataPDU009.CdtTrfTxInf();

        DataPDU009.PmtId pmtId = new DataPDU009.PmtId();
        pmtId.setInstrId(t.messageId());
        pmtId.setEndToEndId("NONREF");
        pmtId.setTxId(t.messageId());
        tx.setPmtId(pmtId);

        tx.setPmtTpInf(buildPmtTpInf009());

        DataPDU009.IntrBkSttlmAmt amt = new DataPDU009.IntrBkSttlmAmt();
        amt.setValue(t.amount().toString());
        amt.setCcy(t.currency());
        tx.setIntrBkSttlmAmt(amt);
        tx.setIntrBkSttlmDt(t.settlementDate());

        DataPDU009.InstgAgt instgAgt = new DataPDU009.InstgAgt();
        DataPDU009.FinInstnId instgFi = new DataPDU009.FinInstnId();
        instgFi.setBicfi(t.instructingAgentBic());
        instgAgt.setFinInstnId(instgFi);
        tx.setInstgAgt(instgAgt);

        DataPDU009.InstdAgt instdAgt = new DataPDU009.InstdAgt();
        DataPDU009.FinInstnId instdFi = new DataPDU009.FinInstnId();
        instdFi.setBicfi(t.instructedAgentBic());
        instdAgt.setFinInstnId(instdFi);
        tx.setInstdAgt(instdAgt);

        // Debtor: financial institution (debtorAgentBic)
        DataPDU009.Dbtr dbtr = new DataPDU009.Dbtr();
        DataPDU009.FinInstnId dbtrFi = new DataPDU009.FinInstnId();
        dbtrFi.setBicfi(t.debtorAgentBic());
        dbtr.setFinInstnId(dbtrFi);
        tx.setDbtr(dbtr);

        DataPDU009.DbtrAcct dbtrAcct = new DataPDU009.DbtrAcct();
        DataPDU009.Id dbtrId = new DataPDU009.Id();
        DataPDU009.Othr dbtrOthr = new DataPDU009.Othr();
        dbtrOthr.setId(t.debtorAgentAccount());
        dbtrId.setOthr(dbtrOthr);
        dbtrAcct.setId(dbtrId);
        tx.setDbtrAcct(dbtrAcct);

        // Creditor: financial institution (instructedAgentBic)
        DataPDU009.Cdtr cdtr = new DataPDU009.Cdtr();
        DataPDU009.FinInstnId cdtrFi = new DataPDU009.FinInstnId();
        cdtrFi.setBicfi(t.instructedAgentBic());
        cdtr.setFinInstnId(cdtrFi);
        tx.setCdtr(cdtr);

        DataPDU009.CdtrAcct cdtrAcct = new DataPDU009.CdtrAcct();
        DataPDU009.Id cdtrId = new DataPDU009.Id();
        DataPDU009.Othr cdtrOthr = new DataPDU009.Othr();
        cdtrOthr.setId(t.creditorAgentAccount());
        cdtrId.setOthr(cdtrOthr);
        cdtrAcct.setId(cdtrId);
        tx.setCdtrAcct(cdtrAcct);

        if (t.instrForNxtAgt() != null) {
            DataPDU009.InstrForNxtAgt instr = new DataPDU009.InstrForNxtAgt();
            instr.setInstrInf(t.instrForNxtAgt());
            tx.setInstrForNxtAgt(instr);
        }

        return tx;
    }

    private DataPDU009.PmtTpInf buildPmtTpInf009() {
        DataPDU009.PmtTpInf pmtTpInf = new DataPDU009.PmtTpInf();
        pmtTpInf.setClrChanl("RTGS");
        DataPDU009.SvcLvl svcLvl = new DataPDU009.SvcLvl();
        svcLvl.setPrtry("0010");
        pmtTpInf.setSvcLvl(svcLvl);
        DataPDU009.LclInstrm lclInstrm = new DataPDU009.LclInstrm();
        lclInstrm.setPrtry("RTGS-FICT");
        pmtTpInf.setLclInstrm(lclInstrm);
        DataPDU009.CtgyPurp ctgyPurp = new DataPDU009.CtgyPurp();
        ctgyPurp.setPrtry("001");
        pmtTpInf.setCtgyPurp(ctgyPurp);
        return pmtTpInf;
    }

    // -------------------------------------------------------------------------
    // Namespace normalisation
    // -------------------------------------------------------------------------

    /**
     * JAXB collects all namespace declarations at the root element with generated prefixes.
     * This converts them to inline default namespace declarations:
     *   <DataPDU xmlns="urn:cma:stp:xsd:stp.1.0">
     *   <AppHdr xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01">
     *   <Document xmlns="urn:iso:std:iso:20022:tech:xsd:{msgType}">
     */
    private String normalizeNamespacePrefixes(String xml, String msgType) {
        Matcher m = Pattern.compile("\\s+xmlns:(\\S+?)=\"([^\"]+)\"").matcher(xml);
        Map<String, String> uriToPrefix = new HashMap<>();
        while (m.find()) {
            uriToPrefix.put(m.group(2), m.group(1));
        }

        String stpNs  = "urn:cma:stp:xsd:stp.1.0";
        String headNs = "urn:iso:std:iso:20022:tech:xsd:head.001.001.01";
        String pacsNs = "urn:iso:std:iso:20022:tech:xsd:" + msgType;

        String stpPfx  = uriToPrefix.get(stpNs);
        String headPfx = uriToPrefix.get(headNs);
        String pacsPfx = uriToPrefix.get(pacsNs);

        String result = xml.replaceAll("\\s+xmlns:\\S+?=\"[^\"]+\"", "");

        if (stpPfx != null) {
            result = result.replace("<"  + stpPfx + ":DataPDU",  "<DataPDU xmlns=\"" + stpNs + "\"");
            result = result.replace("</" + stpPfx + ":DataPDU>", "</DataPDU>");
        }
        if (headPfx != null) {
            result = result.replace("<"  + headPfx + ":AppHdr",  "<AppHdr xmlns=\"" + headNs + "\"");
            result = result.replace("</" + headPfx + ":AppHdr>", "</AppHdr>");
        }
        if (pacsPfx != null) {
            result = result.replace("<"  + pacsPfx + ":Document",  "<Document xmlns=\"" + pacsNs + "\"");
            result = result.replace("</" + pacsPfx + ":Document>", "</Document>");
        }

        return result;
    }
}