package com.noh.stpclient.model.base;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base request structure for all API requests
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRequest<T> {

    @NotBlank(message = "Request ID is required")
//    @JsonProperty("requestId")
    private String requestId;

    @NotBlank(message = "Request datetime is required")
    private String requestDt;

//    @NotNull(message = "Request timestamp is required")
//    @JsonProperty("requestTimestamp")
//    private Instant requestTimestamp = Instant.now();

//    @Valid
//    @NotNull(message = "Client info is required")
//    @JsonProperty("clientInfo")
//    private ClientInfo clientInfo;

//    @Valid
//    @JsonProperty("securityContext")
//    private SecurityContext securityContext;

//    @Valid
//    @NotNull(message = "Data is required")
    @JsonProperty("data")
    @Valid
    private T data;


}
