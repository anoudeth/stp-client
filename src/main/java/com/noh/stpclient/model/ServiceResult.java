package com.noh.stpclient.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResult<T> {
    private boolean success;
    private T data;
    private String errorCode;
    private String errorMessage;

    public static <T> ServiceResult<T> success(T data) {
        return ServiceResult.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ServiceResult<T> failure(String errorCode, String errorMessage) {
        return ServiceResult.<T>builder()
                .success(false)
                .data(null)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
