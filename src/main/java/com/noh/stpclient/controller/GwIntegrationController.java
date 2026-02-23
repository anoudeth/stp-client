package com.noh.stpclient.controller;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ApiRequest;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.service.GwIntegrationService;
import com.noh.stpclient.utils.ApiResponseBuilder;
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

        ServiceResult<LogonResponseDto> serviceResult = gwIntegrationService.performLogon(request.getData().username(), request.getData().password());
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

        // get logon session ID

        // performSend
        ServiceResult<SendResponseDto> serviceResult = gwIntegrationService.performSend(request.getData());
        ApiResponse<SendResponseDto> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        // logout session ID
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
}
