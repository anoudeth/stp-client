package com.noh.stpclient.model.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Server information included in response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInfo {

    @JsonProperty("serverNode")
    private String serverNode;

    @JsonProperty("apiVersion")
    private String apiVersion;

    @JsonProperty("environment")
    private String environment; // production, staging, development
}
