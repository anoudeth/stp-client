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
 * Base res structure for all API ress
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponse<T> {

    @JsonProperty("resCode")
    private String resCode;

    @JsonProperty("resMessage")
    private String resMessage;

    @JsonProperty("resStatus")
    private EResponseStatus resStatus;      // OK, FAILED, PENDING, PARTIAL_SUCCESS

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("resId")
    private String resId;

    @JsonProperty("requestTimestamp")
    private Instant requestTimestamp;

    @JsonProperty("resTimestamp")
    @NotBlank(message = "res timestamp is required")
    private Instant resTimestamp;

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
