package com.noh.stpclient.model.base;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ErrorResponse")
public class ErrorResponse extends BaseResponse<Void> {
    // This class intentionally has no additional fields
    // It's used specifically for error responses where data is null
}
