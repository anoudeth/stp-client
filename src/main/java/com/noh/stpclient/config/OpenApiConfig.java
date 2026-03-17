package com.noh.stpclient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stpClientOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("STP Client Gateway Integration API")
                        .version("1.0.0")
                        .description("""
                                REST API for integrating with the SWIFT Gateway (GWClientMU).
                                Provides endpoints for session management, message retrieval,
                                financial transaction submission, and ACK/NAK processing.
                                """)
                        .contact(new Contact()
                                .name("LBB")
                                .email("support@lbb.com")))
                .servers(List.of(
                        new Server().url("http://localhost:7003/stp-client").description("Local Development")
                ));
    }
}
