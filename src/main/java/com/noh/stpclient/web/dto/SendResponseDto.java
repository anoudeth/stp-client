package com.noh.stpclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noh.stpclient.model.xml.SendResponseData;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendResponseDto(
        String type,
        String datetime,
        String mir,
        String ref,
        String code,
        String description,
        String info
) {
    public static SendResponseDto from(SendResponseData data) {
        return new SendResponseDto(
                data.getType(),
                data.getDatetime(),
                data.getMir(),
                data.getRef(),
                data.getCode(),
                data.getDescription(),
                data.getInfo()
        );
    }
}
