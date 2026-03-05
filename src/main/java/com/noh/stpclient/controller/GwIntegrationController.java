package com.noh.stpclient.controller;

import com.noh.stpclient.model.ApiRequest;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.service.GwIntegrationService;
import com.noh.stpclient.service.SessionManager;
import com.noh.stpclient.utils.ApiResponseBuilder;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.GetUpdatesRequest;
import com.noh.stpclient.web.dto.GetUpdatesResponseDto;
import com.noh.stpclient.web.dto.LogonRequest;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.LogoutRequest;
import com.noh.stpclient.web.dto.SendAckNakRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/gw")
@Slf4j
@AllArgsConstructor
public class GwIntegrationController {

    private final GwIntegrationService gwIntegrationService;
    private final SessionManager sessionManager;
    private final ApiResponseBuilder responseBuilder;

    @PostMapping("/logon")
    public ResponseEntity<ApiResponse<LogonResponseDto>> logon(@Valid @RequestBody ApiRequest<LogonRequest> request) {
        log.info(">>> START logon >>>");

        if (request.getData() == null) {
            ApiResponse<LogonResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<LogonResponseDto> serviceResult = gwIntegrationService.performLogon();
        ApiResponse<LogonResponseDto> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("<<< END logon <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody ApiRequest<LogoutRequest> request) {
        log.info(">>> START logout >>>");

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performLogout(request.getData().sessionId());
        // Invalidate the shared session if the caller explicitly logs out
        sessionManager.invalidate();

        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("<<< END logout <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/get-updates")
    public ResponseEntity<ApiResponse<List<GetUpdatesResponseDto>>> getUpdates(@Valid @RequestBody ApiRequest<GetUpdatesRequest> request) {
        log.info(">>> START getUpdates >>>");

        if (request.getData() == null) {
            ApiResponse<List<GetUpdatesResponseDto>> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<List<GetUpdatesResponseDto>> serviceResult = gwIntegrationService.performGetUpdates(request.getData().sessionId());
        ApiResponse<List<GetUpdatesResponseDto>> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("<<< END getUpdates <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendResponseDto>> send(@Valid @RequestBody ApiRequest<FinancialTransactionRequest> request) {
        log.info(">>> START send >>>");

        if (request.getData() == null) {
            ApiResponse<SendResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        String sessionId = sessionManager.getSession();
        FinancialTransactionRequest transactionRequest = new FinancialTransactionRequest(sessionId, request.getData().transaction());
        ServiceResult<SendResponseDto> srRsSend = gwIntegrationService.performSendFinancialTransaction(transactionRequest);

        ApiResponse<SendResponseDto> finalResponse = srRsSend.isSuccess()
                ? responseBuilder.buildSuccessResponse(srRsSend.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(srRsSend, null, Locale.getDefault());

        log.info("<<< END send <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/send-ack-nak")
    public ResponseEntity<ApiResponse<Void>> sendAckNak(@Valid @RequestBody ApiRequest<SendAckNakRequest> request) {
        log.info(">>> START sendAckNak >>>");

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performSendAckNak(request.getData());
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("<<< END sendAckNak <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/financial-transaction")
    public ResponseEntity<ApiResponse<SendResponseDto>> financialTransaction(@Valid @RequestBody ApiRequest<FinancialTransactionRequest> request) {
        log.info(">>> START financialTransaction >>>");

        if (request.getData() == null) {
            ApiResponse<SendResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        String sessionId = sessionManager.getSession();
        FinancialTransactionRequest transactionRequest = new FinancialTransactionRequest(sessionId, request.getData().transaction());
        ServiceResult<SendResponseDto> srRsTransaction = gwIntegrationService.performFinancialTransaction(transactionRequest);

        ApiResponse<SendResponseDto> finalResponse = srRsTransaction.isSuccess()
                ? responseBuilder.buildSuccessResponse(srRsTransaction.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(srRsTransaction, null, Locale.getDefault());

        log.info("<<< END financialTransaction <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
