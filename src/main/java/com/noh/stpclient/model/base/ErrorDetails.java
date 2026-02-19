package com.noh.stpclient.model.base;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("errorCategory")
    private String errorCategory; // VALIDATION, BUSINESS, TECHNICAL, SECURITY

    @JsonProperty("fieldErrors")
    private List<FieldError> fieldErrors;

    @JsonProperty("errorDetails")
    private Map<String, Object> errorDetails;
}