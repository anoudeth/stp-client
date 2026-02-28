package com.noh.stpclient.service;

import com.noh.stpclient.model.xml.DataPDU;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for transforming financial transaction data to DataPDU XML format.
 */
@Service
@Slf4j
public class DataPDUTransformer {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Transforms FinancialTransactionRequest to DataPDU object.
     */
    public DataPDU transformToDataPDU(FinancialTransactionRequest request) {
        log.debug("Transforming financial transaction request to DataPDU format");
        
        var transaction = request.transaction();
        String currentDateTime = ZonedDateTime.now().format(ISO_DATE_TIME);
        
        DataPDU dataPDU = new DataPDU();
        DataPDU.Body body = new DataPDU.Body();
        
        // Create AppHdr
        DataPDU.AppHdr appHdr = createAppHdr(transaction, currentDateTime);
        body.setAppHdr(appHdr);
        
        // Create Document
        DataPDU.Document document = createDocument(transaction, currentDateTime);
        body.setDocument(document);
        
        dataPDU.setBody(body);
        
        return dataPDU;
    }

    /**
     * Creates AppHdr section.
     */
    private DataPDU.AppHdr createAppHdr(FinancialTransactionRequest.TransactionData transaction, String currentDateTime) {
        DataPDU.AppHdr appHdr = new DataPDU.AppHdr();
        
        // From (Fr)
        DataPDU.Fr fr = new DataPDU.Fr();
        DataPDU.FIId frFiId = new DataPDU.FIId();
        DataPDU.FinInstnId frFinInstnId = new DataPDU.FinInstnId();
        frFinInstnId.setBicfi(transaction.senderBic());
        frFiId.setFinInstnId(frFinInstnId);
        fr.setFiId(frFiId);
        appHdr.setFr(fr);
        
        // To
        DataPDU.To to = new DataPDU.To();
        DataPDU.FIId toFiId = new DataPDU.FIId();
        DataPDU.FinInstnId toFinInstnId = new DataPDU.FinInstnId();
        toFinInstnId.setBicfi(transaction.receiverBic());
        toFiId.setFinInstnId(toFinInstnId);
        to.setFiId(toFiId);
        appHdr.setTo(to);
        
        appHdr.setBizMsgIdr(transaction.businessMessageId());
        appHdr.setMsgDefIdr("pacs.008.001.08");
        appHdr.setBizSvc("RTGS");
        appHdr.setCreDt(currentDateTime);
        
        return appHdr;
    }

    /**
     * Creates Document section.
     */
    private DataPDU.Document createDocument(FinancialTransactionRequest.TransactionData transaction, String currentDateTime) {
        DataPDU.Document document = new DataPDU.Document();
        DataPDU.FIToFICstmrCdtTrf fiToFICstmrCdtTrf = new DataPDU.FIToFICstmrCdtTrf();
        
        // Group Header
        DataPDU.GrpHdr grpHdr = createGroupHeader(transaction, currentDateTime);
        fiToFICstmrCdtTrf.setGrpHdr(grpHdr);
        
        // Credit Transfer Transaction Info
        DataPDU.CdtTrfTxInf cdtTrfTxInf = createCreditTransferTransactionInfo(transaction);
        fiToFICstmrCdtTrf.setCdtTrfTxInf(cdtTrfTxInf);
        
        document.setFiToFICstmrCdtTrf(fiToFICstmrCdtTrf);
        
        return document;
    }

    /**
     * Creates Group Header section.
     */
    private DataPDU.GrpHdr createGroupHeader(FinancialTransactionRequest.TransactionData transaction, String currentDateTime) {
        DataPDU.GrpHdr grpHdr = new DataPDU.GrpHdr();
        
        grpHdr.setMsgId(transaction.messageId());
        grpHdr.setCreDtTm(currentDateTime.substring(0, 19)); // Remove milliseconds
        grpHdr.setNbOfTxs("1");
        
        DataPDU.SttlmInf sttlmInf = new DataPDU.SttlmInf();
        sttlmInf.setSttlmMtd("CLRG");
        grpHdr.setSttlmInf(sttlmInf);
        
        return grpHdr;
    }

    /**
     * Creates Credit Transfer Transaction Info section.
     */
    private DataPDU.CdtTrfTxInf createCreditTransferTransactionInfo(FinancialTransactionRequest.TransactionData transaction) {
        DataPDU.CdtTrfTxInf cdtTrfTxInf = new DataPDU.CdtTrfTxInf();
        
        // Payment ID
        DataPDU.PmtId pmtId = new DataPDU.PmtId();
        pmtId.setInstrId(transaction.messageId());
        pmtId.setEndToEndId("NOTPROVIDED");
        pmtId.setTxId(transaction.messageId());
        cdtTrfTxInf.setPmtId(pmtId);
        
        // Payment Type Information
        DataPDU.PmtTpInf pmtTpInf = createPaymentTypeInformation();
        cdtTrfTxInf.setPmtTpInf(pmtTpInf);
        
        // Interbank Settlement Amount
        DataPDU.IntrBkSttlmAmt intrBkSttlmAmt = new DataPDU.IntrBkSttlmAmt();
        intrBkSttlmAmt.setValue(transaction.amount().toString());
        intrBkSttlmAmt.setCcy(transaction.currency());
        cdtTrfTxInf.setIntrBkSttlmAmt(intrBkSttlmAmt);
        
        cdtTrfTxInf.setIntrBkSttlmDt(transaction.settlementDate());
        cdtTrfTxInf.setChrgBr("SHAR");
        
        // Instructing Agent
        DataPDU.InstgAgt instgAgt = new DataPDU.InstgAgt();
        DataPDU.FinInstnId instgFinInstnId = new DataPDU.FinInstnId();
        instgFinInstnId.setBicfi(transaction.instructingAgentBic());
        instgAgt.setFinInstnId(instgFinInstnId);
        cdtTrfTxInf.setInstgAgt(instgAgt);
        
        // Instructed Agent
        DataPDU.InstdAgt instdAgt = new DataPDU.InstdAgt();
        DataPDU.FinInstnId instdFinInstnId = new DataPDU.FinInstnId();
        instdFinInstnId.setBicfi(transaction.instructedAgentBic());
        instdAgt.setFinInstnId(instdFinInstnId);
        cdtTrfTxInf.setInstdAgt(instdAgt);
        
        // Debtor
        DataPDU.Dbtr dbtr = new DataPDU.Dbtr();
        dbtr.setNm(transaction.debtorName());
        DataPDU.PstlAdr dbtrPstlAdr = new DataPDU.PstlAdr();
        if (transaction.debtorAddressLines() != null && !transaction.debtorAddressLines().isEmpty()) {
            dbtrPstlAdr.setAdrLine(transaction.debtorAddressLines());
        }
        dbtr.setPstlAdr(dbtrPstlAdr);
        cdtTrfTxInf.setDbtr(dbtr);
        
        // Debtor Account
        DataPDU.DbtrAcct dbtrAcct = new DataPDU.DbtrAcct();
        DataPDU.Id dbtrId = new DataPDU.Id();
        DataPDU.Othr dbtrOthr = new DataPDU.Othr();
        dbtrOthr.setId(transaction.debtorAccount());
        dbtrId.setOthr(dbtrOthr);
        dbtrAcct.setId(dbtrId);
        cdtTrfTxInf.setDbtrAcct(dbtrAcct);
        
        // Debtor Agent
        DataPDU.DbtrAgt dbtrAgt = new DataPDU.DbtrAgt();
        DataPDU.FinInstnId dbtrFinInstnId = new DataPDU.FinInstnId();
        dbtrFinInstnId.setBicfi(transaction.debtorAgentBic());
        dbtrAgt.setFinInstnId(dbtrFinInstnId);
        cdtTrfTxInf.setDbtrAgt(dbtrAgt);
        
        // Debtor Agent Account
        DataPDU.DbtrAgtAcct dbtrAgtAcct = new DataPDU.DbtrAgtAcct();
        DataPDU.Id dbtrAgtId = new DataPDU.Id();
        DataPDU.Othr dbtrAgtOthr = new DataPDU.Othr();
        dbtrAgtOthr.setId(transaction.debtorAgentAccount());
        dbtrAgtId.setOthr(dbtrAgtOthr);
        dbtrAgtAcct.setId(dbtrAgtId);
        cdtTrfTxInf.setDbtrAgtAcct(dbtrAgtAcct);
        
        // Creditor Agent
        DataPDU.CdtrAgt cdtrAgt = new DataPDU.CdtrAgt();
        DataPDU.FinInstnId cdtrFinInstnId = new DataPDU.FinInstnId();
        cdtrFinInstnId.setBicfi(transaction.instructedAgentBic());
        cdtrAgt.setFinInstnId(cdtrFinInstnId);
        cdtTrfTxInf.setCdtrAgt(cdtrAgt);
        
        // Creditor Agent Account
        DataPDU.CdtrAgtAcct cdtrAgtAcct = new DataPDU.CdtrAgtAcct();
        DataPDU.Id cdtrAgtId = new DataPDU.Id();
        DataPDU.Othr cdtrAgtOthr = new DataPDU.Othr();
        cdtrAgtOthr.setId(transaction.creditorAgentAccount());
        cdtrAgtId.setOthr(cdtrAgtOthr);
        cdtrAgtAcct.setId(cdtrAgtId);
        cdtTrfTxInf.setCdtrAgtAcct(cdtrAgtAcct);
        
        // Creditor
        DataPDU.Cdtr cdtr = new DataPDU.Cdtr();
        cdtr.setNm(transaction.creditorName());
        cdtr.setPstlAdr(new DataPDU.PstlAdr());
        cdtTrfTxInf.setCdtr(cdtr);
        
        // Creditor Account
        DataPDU.CdtrAcct cdtrAcct = new DataPDU.CdtrAcct();
        DataPDU.Id cdtrId = new DataPDU.Id();
        DataPDU.Othr cdtrOthr = new DataPDU.Othr();
        cdtrOthr.setId(transaction.creditorAccount());
        cdtrId.setOthr(cdtrOthr);
        cdtrAcct.setId(cdtrId);
        cdtTrfTxInf.setCdtrAcct(cdtrAcct);
        
        // Instruction for Next Agent
        if (transaction.instrForNxtAgt() != null) {
            DataPDU.InstrForNxtAgt instr = new DataPDU.InstrForNxtAgt();
            instr.setInstrInf(transaction.instrForNxtAgt());
            cdtTrfTxInf.setInstrForNxtAgt(instr);
        }

        // Remittance Information
        DataPDU.RmtInf rmtInf = new DataPDU.RmtInf();
        rmtInf.setUstrd(transaction.remittanceInformation());
        cdtTrfTxInf.setRmtInf(rmtInf);
        
        return cdtTrfTxInf;
    }

    /**
     * Creates Payment Type Information section.
     */
    private DataPDU.PmtTpInf createPaymentTypeInformation() {
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

    /**
     * Marshals DataPDU object to XML string.
     */
    public String marshalToXml(DataPDU dataPDU) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(DataPDU.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        
        StringWriter writer = new StringWriter();
        marshaller.marshal(dataPDU, writer);
        
        String xml = writer.toString();
        log.debug("Generated XML: {}", xml);
        
        return xml;
    }
}
