package com.noh.stpclient.model.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {

    @JsonProperty("field")
    private String field;

    @JsonProperty("rejectedValue")
    private Object rejectedValue;

    @JsonProperty("message")
    private String message;

    @JsonProperty("code")
    private String code;
}