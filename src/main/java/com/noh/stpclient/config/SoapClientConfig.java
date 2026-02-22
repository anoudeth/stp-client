package com.noh.stpclient.config;

import com.noh.stpclient.config.interceptor.SoapPayloadLoggingInterceptor;
import com.noh.stpclient.remote.GWClientMuRemote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

@Configuration
public class SoapClientConfig {

    private static final String TARGET_URL = "http://172.16.2.8:7080/GWClientMUService/GWClientMU";

    @Bean
    public Jaxb2Marshaller marshaller() {
        final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // Use specific classes instead of context path
        marshaller.setClassesToBeBound(
            com.noh.stpclient.model.xml.Logon.class,
            com.noh.stpclient.model.xml.LogonResponse.class,
            com.noh.stpclient.model.xml.Send.class,
            com.noh.stpclient.model.xml.Logout.class,
            com.noh.stpclient.model.xml.GetUpdates.class,
            com.noh.stpclient.model.xml.SendAckNak.class
        );
        return marshaller;
    }

    @Bean
    public GWClientMuRemote gwClientMuRemote(final Jaxb2Marshaller marshaller) {
        final GWClientMuRemote client = new GWClientMuRemote();
        client.setDefaultUri(TARGET_URL);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        // Register the interceptor to log all outgoing/incoming SOAP messages
        client.setInterceptors(new ClientInterceptor[]{new SoapPayloadLoggingInterceptor()});
        
        // Correctly set the fault message resolver on the template
        client.getWebServiceTemplate().setFaultMessageResolver(new CustomSoapFaultMessageResolver());
        
        return client;
    }
}
