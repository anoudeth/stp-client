package com.noh.stpclient.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noh.stpclient.model.base.BaseResponse;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class ApiResponse<T> extends BaseResponse<T> {
    // 1. This is the correct, high-performance, and correctly configured pattern.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules() // This finds and registers the Java 8 time module (JSR-310)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Produces readable ISO-8601 strings

    @Override
    public String toString() {
        try {
            // 2. Now using the correctly configured mapper.
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // This is a safer way to handle exceptions in toString()
            return "Error converting to JSON: " + e.getMessage();
        }
    }
}
