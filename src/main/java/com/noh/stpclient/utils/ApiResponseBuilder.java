package com.noh.stpclient.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.EResponseStatus;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.base.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

/**
 * A centralized component for building consistent API responses.
 * This promotes code reuse and ensures all responses follow the same structure.
 */
@Component
@RequiredArgsConstructor
public class ApiResponseBuilder {

    private final MessageSource msgSource;
    private final ObjectMapper objectMapper;

    /**
     * Builds a standardized success response.
     *
     * @param data       The payload to be returned.
     * @param clientInfo The client info from the original request.
     * @param locale     The user's locale for message localization.
     * @return A fully constructed success ApiResponse.
     */
    public <T> ApiResponse<T> buildSuccessResponse(T data, ClientInfo clientInfo, Locale locale) {
        return ApiResponse.<T>builder()
                .responseCode(msgSource.getMessage("success.completed.code", null, "0000", locale))
                .responseMessage(msgSource.getMessage("success.completed.msg", null, "Success", locale))
                .responseStatus(EResponseStatus.SUCCESS)
                .responseTimestamp(Instant.now())
                .clientInfo(clientInfo)
                .data(data)
                .build();
    }

//    /**
//     * Builds a specialized success response for QR generation, returning only the qrString.
//     *
//     * @param qrString   The generated QR string.
//     * @param clientInfo The client info from the original request.
//     * @param locale     The user's locale for message localization.
//     * @return A success ApiResponse containing a simple JSON object with the qrString.
//     */
//    public ApiResponse<ObjectNode> buildQrSuccessResponse(String qrString, ClientInfo clientInfo, Locale locale) {
//        ObjectNode dataNode = objectMapper.createObjectNode();
//        dataNode.put("qrString", qrString);
//        return buildSuccessResponse(dataNode, clientInfo, locale);
//    }

    /**
     * Builds a standardized and safe failure response from a ServiceResult.
     *
     * @param serviceResult The result from the service layer containing error codes.
     * @param clientInfo    The client info from the original request.
     * @param locale        The user's locale for message localization.
     * @return A fully constructed failure ApiResponse.
     */
    public <T> ApiResponse<T> buildFailureResponse(ServiceResult<?> serviceResult, ClientInfo clientInfo, Locale locale) {
        // Safely resolve the response CODE, providing a generic fallback.
        String responseCode = msgSource.getMessage(
                serviceResult.getErrorCode(),    // The code key (e.g., "qr.invalid.code" or error key code that not available in messages.properties )
                null,
                serviceResult.getErrorCode(),                          // A generic fallback code
                locale
        );

        // Safely resolve the response MESSAGE, providing a generic fallback.
        String responseMessage = msgSource.getMessage(
                serviceResult.getErrorMessage(), // The message key (e.g. error key message that not available in messages.properties)
                null,
                serviceResult.getErrorMessage(), // default error message
                locale
        );

        return ApiResponse.<T>builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .responseStatus(EResponseStatus.FAILED)
                .clientInfo(clientInfo)
                .responseTimestamp(Instant.now())
//                .data(serviceResult.getData())
                // NOTE: We intentionally do not include 'data' in a failure response.
                .build();
    }
}