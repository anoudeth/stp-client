package com.noh.stpclient.exception;

import lombok.Getter;

@Getter
public class GatewayIntegrationException extends RuntimeException {
    private final String code;
    private final String description;
    private final String info;

    public GatewayIntegrationException(String code, String description, String info) {
        super(String.format("Gateway Error [%s]: %s - %s", code, description, info));
        this.code = code;
        this.description = description;
        this.info = info;
    }
}
