package com.noh.stpclient.config;

import com.noh.stpclient.remote.GWClientMuRemote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class SoapClientConfig {

    private static final String TARGET_URL = "http://172.16.2.8:7080/GWClientMUService/GWClientMU";

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
    public GWClientMuRemote gwClientMuRemote(Jaxb2Marshaller marshaller) {
        final GWClientMuRemote client = new GWClientMuRemote();
        client.setDefaultUri(TARGET_URL);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}