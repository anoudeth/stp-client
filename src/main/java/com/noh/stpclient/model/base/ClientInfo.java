package com.noh.stpclient.model.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client device and session information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientInfo {

    @Valid
    @NotBlank(message = "Device ID is required")
    @JsonProperty("deviceId")
    private String deviceId;
    @Valid
    @NotBlank(message = "Mobile number is required")
    @JsonProperty("mobileNo")
    private String mobileNo;
    @Valid
    @NotBlank(message = "User ID is required")
    @JsonProperty("userId")
    private String userId;

    //    @NotBlank(message = "Session ID is required")
    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("appVersion")
    private String appVersion;

    @JsonProperty("platform")
    private String platform; // iOS, Android, Web

    @JsonProperty("osVersion")
    private String osVersion;

    //    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Invalid locale format")
    @JsonProperty("locale")
    private String locale; // en-US, fr-FR, etc.

    @JsonProperty("timezone")
    private String timezone; // America/New_York
}