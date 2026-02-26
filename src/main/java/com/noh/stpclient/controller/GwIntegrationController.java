package com.noh.stpclient.controller;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ApiRequest;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.service.GwIntegrationService;
import com.noh.stpclient.utils.ApiResponseBuilder;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.GetUpdatesRequest;
import com.noh.stpclient.web.dto.LogonRequest;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.LogoutRequest;
import com.noh.stpclient.web.dto.SendRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import com.noh.stpclient.web.dto.SendAckNakRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/gw")
@Slf4j
@AllArgsConstructor
public class GwIntegrationController {

    private final GwIntegrationService gwIntegrationService;
    private final ApiResponseBuilder responseBuilder;

    @PostMapping("/logon")
    public ResponseEntity<ApiResponse<LogonResponseDto>> logon(@Valid @RequestBody ApiRequest<LogonRequest> request) {
        log.info(">>> START logon >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<LogonResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<LogonResponseDto> serviceResult = gwIntegrationService.performLogon();
        ApiResponse<LogonResponseDto> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END logon request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody ApiRequest<LogoutRequest> request) {
        log.info(">>> START logout >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performLogout(request.getData().sessionId());
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END logout request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/get-updates")
    public ResponseEntity<ApiResponse<Void>> getUpdates(@Valid @RequestBody ApiRequest<GetUpdatesRequest> request) {
        log.info(">>> START getUpdates >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performGetUpdates(request.getData().sessionId());
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END getUpdates request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendResponseDto>> send(@Valid @RequestBody ApiRequest<SendRequest> request) {
        log.info(">>> START send >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<SendResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        // 1. get logon session ID
        ServiceResult<LogonResponseDto> svRsLogon = gwIntegrationService.performLogon();
        if (!svRsLogon.isSuccess()) {
            ApiResponse<SendResponseDto> failureResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(svRsLogon.getErrorCode(), svRsLogon.getErrorMessage()), null, Locale.getDefault());
            return ResponseEntity.ok(failureResponse);
        }

        final String sessionId = svRsLogon.getData().sessionId();
        ServiceResult<SendResponseDto> srRsSend;

        try {
            // 2. performSend
            // 2.1 set session ID
            SendRequest sendRequest = new SendRequest(sessionId, request.getData().message());
            srRsSend = gwIntegrationService.performSend(sendRequest);
        } finally {
            // 3. logout session ID
            gwIntegrationService.performLogout(sessionId);
        }
        
        ApiResponse<SendResponseDto> finalResponse = srRsSend.isSuccess()
                ? responseBuilder.buildSuccessResponse(srRsSend.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(srRsSend, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END send request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/send-ack-nak")
    public ResponseEntity<ApiResponse<Void>> sendAckNak(@Valid @RequestBody ApiRequest<SendAckNakRequest> request) {
        log.info(">>> START sendAckNak >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performSendAckNak(request.getData().messageId(), request.getData().isAck());
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END sendAckNak request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/financial-transaction")
    public ResponseEntity<ApiResponse<String>> financialTransaction(@Valid @RequestBody ApiRequest<FinancialTransactionRequest> request) {
        log.info(">>> START financialTransaction >>>");
        log.info("> request body: {}", request);

        if (request.getData() == null) {
            ApiResponse<String> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        // 1. get logon session ID
        ServiceResult<LogonResponseDto> svRsLogon = gwIntegrationService.performLogon();
        if (!svRsLogon.isSuccess()) {
            ApiResponse<String> failureResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(svRsLogon.getErrorCode(), svRsLogon.getErrorMessage()), null, Locale.getDefault());
            return ResponseEntity.ok(failureResponse);
        }

        final String sessionId = svRsLogon.getData().sessionId();
        ServiceResult<String> srRsTransaction;

        try {
            // 2. perform financial transaction
            // 2.1 set session ID
            FinancialTransactionRequest transactionRequest = new FinancialTransactionRequest(sessionId, request.getData().transaction());
            srRsTransaction = gwIntegrationService.performFinancialTransaction(transactionRequest);
        } finally {
            // 3. logout session ID
            gwIntegrationService.performLogout(sessionId);
        }
        
        ApiResponse<String> finalResponse = srRsTransaction.isSuccess()
                ? responseBuilder.buildSuccessResponse(srRsTransaction.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(srRsTransaction, null, Locale.getDefault());

        log.info("< Final response: {}", finalResponse);
        log.info("<<< END financialTransaction request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
