package com.noh.stpclient.controller;

import com.noh.stpclient.model.ApiRequest;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.service.GwIntegrationService;
import com.noh.stpclient.utils.ApiResponseBuilder;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.GetUpdatesResponseDto;
import com.noh.stpclient.web.dto.LogonRequest;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.LogoutRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import com.noh.stpclient.web.dto.SendAckNakRequest;
import com.noh.stpclient.web.dto.SendXmlFileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@Tag(name = "Gateway Integration", description = "Endpoints for interacting with the SWIFT GWClientMU gateway: session management, message updates, financial transactions, and ACK/NAK processing.")
public class GwIntegrationController {

    private final GwIntegrationService gwIntegrationService;
    private final ApiResponseBuilder responseBuilder;

    @Operation(summary = "Logon to gateway", description = "Authenticates with the SWIFT gateway using configured credentials and returns a session ID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logon successful or failed (check resStatus in body)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request data is null or invalid")
    })
    @PostMapping("/logon")
    public ResponseEntity<ApiResponse<LogonResponseDto>> logon(@Valid @RequestBody ApiRequest<LogonRequest> request) {
        log.info(">>> START logon >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        if (request.getData() == null) {
            ApiResponse<LogonResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<LogonResponseDto> serviceResult = gwIntegrationService.performLogon();
        ApiResponse<LogonResponseDto> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END logon request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @Operation(summary = "Logout from gateway", description = "Terminates the current gateway session identified by the provided session ID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful or failed (check resStatus in body)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request data is null or invalid")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody ApiRequest<LogoutRequest> request) {
        log.info(">>> START logout >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performLogout();
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END logout request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @Operation(summary = "Get message updates", description = "Authenticates with the gateway, polls for pending inbound SWIFT messages, and returns them as a list. Each item contains full message metadata and block4 content.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Update list returned (may be empty if no pending messages)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/get-updates")
    public ResponseEntity<ApiResponse<List<GetUpdatesResponseDto>>> getUpdates(@Valid @RequestBody ApiRequest<Void> request) {
        log.info(">>> START getUpdates >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        ServiceResult<LogonResponseDto> svRsLogon = gwIntegrationService.performLogon();
        if (!svRsLogon.isSuccess()) {
            ApiResponse<List<GetUpdatesResponseDto>> failureResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(svRsLogon.getErrorCode(), svRsLogon.getErrorMessage()), null, Locale.getDefault());
            return ResponseEntity.ok(failureResponse);
        }

        final String sessionId = svRsLogon.getData().sessionId();
        ServiceResult<List<GetUpdatesResponseDto>> serviceResult = gwIntegrationService.performGetUpdates(sessionId);

        ApiResponse<List<GetUpdatesResponseDto>> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END getUpdates request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @Operation(summary = "Send financial transaction", description = "Authenticates with the gateway and submits a SWIFT financial transaction (pacs.008 / RTGS). Returns the gateway's acknowledgment including MIR and reference code.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction submitted (check resStatus; SUCCESS means gateway accepted)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request data is null or validation failed")
    })
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendResponseDto>> send(@Valid @RequestBody ApiRequest<FinancialTransactionRequest> request) {
        log.info(">>> START send >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        if (request.getData() == null) {
            ApiResponse<SendResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<LogonResponseDto> svRsLogon = gwIntegrationService.performLogon();
        if (!svRsLogon.isSuccess()) {
            ApiResponse<SendResponseDto> failureResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(svRsLogon.getErrorCode(), svRsLogon.getErrorMessage()), null, Locale.getDefault());
            return ResponseEntity.ok(failureResponse);
        }

        final String sessionId = svRsLogon.getData().sessionId();
        FinancialTransactionRequest transactionRequest = new FinancialTransactionRequest(sessionId, request.getData().transaction());
        ServiceResult<SendResponseDto> srRsSend = gwIntegrationService.performSendFinancialTransaction(transactionRequest);

        ApiResponse<SendResponseDto> finalResponse = srRsSend.isSuccess()
                ? responseBuilder.buildSuccessResponse(srRsSend.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(srRsSend, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END send request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @Operation(summary = "Send from XML file", description = "Reads a saved raw XML file from the xmlfile/ directory, signs it with XAdES, and sends it to the gateway. Used to investigate signature failures by bypassing XML generation.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction submitted (check resStatus; SUCCESS means gateway accepted)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request data is null or validation failed")
    })
    @PostMapping("/send-xml-file")
    public ResponseEntity<ApiResponse<SendResponseDto>> sendXmlFile(@Valid @RequestBody ApiRequest<SendXmlFileRequest> request) {
        log.info(">>> START sendXmlFile >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        if (request.getData() == null) {
            ApiResponse<SendResponseDto> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<LogonResponseDto> svRsLogon = gwIntegrationService.performLogon();
        if (!svRsLogon.isSuccess()) {
            ApiResponse<SendResponseDto> failureResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(svRsLogon.getErrorCode(), svRsLogon.getErrorMessage()), null, Locale.getDefault());
            return ResponseEntity.ok(failureResponse);
        }

        final String sessionId = svRsLogon.getData().sessionId();
        ServiceResult<SendResponseDto> serviceResult = gwIntegrationService.performSendFromXmlFile(sessionId, request.getData());

        ApiResponse<SendResponseDto> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END sendXmlFile request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @Operation(summary = "Send ACK or NAK", description = "Sends an acknowledgment (ACK) or negative acknowledgment (NAK) for a received SWIFT message identified by its MIR and datetime. Session is managed internally.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ACK/NAK sent successfully or failed (check resStatus in body)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request data is null or type is not ACK/NAK")
    })
    @PostMapping("/send-ack-nak")
    public ResponseEntity<ApiResponse<Void>> sendAckNak(@Valid @RequestBody ApiRequest<SendAckNakRequest> request) {
        log.info(">>> START sendAckNak >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        if (request.getData() == null) {
            ApiResponse<Void> finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("VALIDATION-001", "Request data cannot be null"), null, Locale.getDefault());
            return ResponseEntity.badRequest().body(finalResponse);
        }

        ServiceResult<Void> serviceResult = gwIntegrationService.performSendAckNak(request.getData());
        ApiResponse<Void> finalResponse = serviceResult.isSuccess()
                ? responseBuilder.buildSuccessResponse(null, null, Locale.getDefault())
                : responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END sendAckNak request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
