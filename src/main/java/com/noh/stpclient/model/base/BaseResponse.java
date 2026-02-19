package com.noh.stpclient.model.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.noh.stpclient.model.EResponseStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base response structure for all API responses
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponse<T> {

    @JsonProperty("responseCode")
    private String responseCode;

    @JsonProperty("responseMessage")
    private String responseMessage;

    @JsonProperty("responseStatus")
    private EResponseStatus responseStatus;      // OK, FAILED, PENDING, PARTIAL_SUCCESS

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("responseId")
    private String responseId;

    @JsonProperty("requestTimestamp")
    private Instant requestTimestamp;

    @JsonProperty("responseTimestamp")
    @NotBlank(message = "Response timestamp is required")
    private Instant responseTimestamp;

    @JsonProperty("processingTime")
    private Long processingTime; // in milliseconds

    @JsonProperty("clientInfo")
    private ClientInfo clientInfo;

    @JsonProperty("serverInfo")
    private ServerInfo serverInfo;

    @JsonProperty("data")
    private T data;

    @JsonProperty("error")
    private ErrorDetails error;

//    @JsonProperty("error")
//    private ErrorDetails error;

//    @JsonProperty("warnings")
//    private List<Warning> warnings;

//    @JsonProperty("links")
//    private Map<String, String> links;
}
