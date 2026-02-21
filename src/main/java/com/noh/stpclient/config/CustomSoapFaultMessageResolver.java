package com.noh.stpclient.config;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.xml.FaultDetail;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapFaultDetail;
import org.springframework.ws.soap.SoapFaultDetailElement;
import org.springframework.ws.soap.SoapMessage;

import javax.xml.transform.Source;
import java.io.IOException;
import java.util.Iterator;

public class CustomSoapFaultMessageResolver implements FaultMessageResolver {

    @Override
    public void resolveFault(WebServiceMessage message) throws IOException {
        if (message instanceof SoapMessage) {
            SoapMessage soapMessage = (SoapMessage) message;
            SoapFault soapFault = soapMessage.getSoapBody().getFault();
            if (soapFault != null) {
                SoapFaultDetail detail = soapFault.getFaultDetail();
                if (detail != null) {
                    Iterator<SoapFaultDetailElement> detailEntries = detail.getDetailEntries();
                    if (detailEntries.hasNext()) {
                        Source detailSource = detailEntries.next().getSource();
                        try {
                            JAXBContext context = JAXBContext.newInstance(FaultDetail.class);
                            Unmarshaller unmarshaller = context.createUnmarshaller();
                            FaultDetail faultDetail = (FaultDetail) unmarshaller.unmarshal(detailSource);
                            throw new GatewayIntegrationException(faultDetail.getCode(), faultDetail.getDescription(), faultDetail.getInfo());
                        } catch (JAXBException e) {
                            // Fallback to default fault string if parsing fails
                            throw new org.springframework.ws.soap.client.SoapFaultClientException(soapMessage);
                        }
                    }
                }
            }
            // Throw generic exception if no detail is found or parsing fails
            throw new org.springframework.ws.soap.client.SoapFaultClientException(soapMessage);
        }
    }
}
