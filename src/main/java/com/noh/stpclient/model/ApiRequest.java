package com.noh.stpclient.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noh.stpclient.model.base.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A generic request class that extends the base request.
 * It can be used to add more fields common to all requests.
 */
@SuperBuilder
@NoArgsConstructor
public class ApiRequest<T> extends BaseRequest<T> {
    // Apply the same world-class pattern here
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Throwing a RuntimeException from toString() can crash services. This is safer.
            return "Error converting to JSON: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
//        ApiRequest<TransDetails> request = new ApiRequest<>();
//
//        TransDetails transDetails = TransDetails.builder()
//                .fromAcctId("019999")
//                .toAcctId("018888")
//                .build();
//
//        request.setRequestId("req1234567");
//        request.setClientInfo(ClientInfo.builder().deviceId("device1234567").build());
//
//        request.setData(transDetails);

///////////////////////////////////////////////////

//        ApiRequest<TransDetails> request1 = ApiRequest.<TransDetails>builder()
//                .clientInfo(ClientInfo.builder()
//                        .deviceId("device1234567")
//                        .mobileNo("2059355555")
//                        .build())
//                .securityContext(SecurityContext.builder()
//                        .channel("MOBILE")
//                        .build())
//                .data(TransDetails.builder()
//                        .fromAcctId("0100001179462")
//                        .toAcctId("326541215402")
//                        .toCustName("Pupe")
//                        .purpose("Pay for food")
//                        .txnAmount(BigDecimal.valueOf(20000))
//                        .txnCcy("LAK")
//                        .txnType(ETxnType.LMPOTA)
//                        .toMemberId("LMPS")
//                        .build())
//                .build();

//        System.out.println(request1);

    }
}