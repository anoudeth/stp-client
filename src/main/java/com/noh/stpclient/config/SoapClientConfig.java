package com.noh.stpclient.config;

import com.noh.stpclient.config.interceptor.SoapPayloadLoggingInterceptor;
import com.noh.stpclient.remote.GWClientMuRemote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

@Configuration
public class SoapClientConfig {

    @Value("${stp.soap.url}")
    private String targetUrl;

    @Bean
    public Jaxb2Marshaller marshaller() {
        final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // Use specific classes instead of context path
        marshaller.setClassesToBeBound(
            com.noh.stpclient.model.xml.Logon.class,
            com.noh.stpclient.model.xml.LogonResponse.class
        );
        return marshaller;
    }

    @Bean
    public GWClientMuRemote gwClientMuRemote(final Jaxb2Marshaller marshaller) {
        final GWClientMuRemote client = new GWClientMuRemote();
        client.setDefaultUri(targetUrl);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        // Register the interceptor to log all outgoing/incoming SOAP messages
        client.setInterceptors(new ClientInterceptor[]{new SoapPayloadLoggingInterceptor()});
        // Set the custom fault message resolver
        client.getWebServiceTemplate().setFaultMessageResolver(new CustomSoapFaultMessageResolver());
        return client;
    }
}
