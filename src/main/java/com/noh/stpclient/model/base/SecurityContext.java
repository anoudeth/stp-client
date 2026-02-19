package com.noh.stpclient.model.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Security context for the request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityContext {

//    @NotBlank(message = "Channel is required")
    @JsonProperty("channel")
    private String channel; // MOBILE_APP, WEB, ATM, BRANCH

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @Valid
    @JsonProperty("geoLocation")
    private GeoLocation geoLocation;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("authenticationMethod")
    private String authenticationMethod; // PIN, BIOMETRIC, PIN_AND_OTP
}