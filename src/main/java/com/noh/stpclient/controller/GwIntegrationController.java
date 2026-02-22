package com.noh.stpclient.controller;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ApiRequest;
import com.noh.stpclient.model.ApiResponse;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.service.GwIntegrationService;
import com.noh.stpclient.utils.ApiResponseBuilder;
import com.noh.stpclient.web.dto.LogonRequest;
import com.noh.stpclient.web.dto.LogonResponseDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * REST Controller for Gateway integration operations.
 * Handles incoming HTTP requests related to Gateway services.
 */
@RestController
@RequestMapping("/gw")
@Slf4j
@AllArgsConstructor
public class GwIntegrationController {

    private final GwIntegrationService gwIntegrationService;
    private final ApiResponseBuilder responseBuilder; // 1. Inject the new builder

    /**
     * Handles the POST request for user logon to the Gateway.
     *
     * @param request The logon request containing username and password.
     * @return A ResponseEntity containing the session ID on success, or an error status.
     */
    @PostMapping("/logon")
    public ResponseEntity<?> logon(@Valid @RequestBody ApiRequest<LogonRequest> request) {
        log.info(">>> START logon >>>");
        log.info("> request body: {}", request);
        ApiResponse<LogonResponseDto> finalResponse = new ApiResponse<>();
        ServiceResult<LogonResponseDto> serviceResult = new ServiceResult<>();
        try {

            serviceResult = gwIntegrationService.performLogon(request.getData().username(), request.getData().password());

            if (serviceResult.isSuccess()) {
                finalResponse = responseBuilder.buildSuccessResponse(serviceResult.getData(), null, Locale.getDefault());
            } else {
                finalResponse = responseBuilder.buildFailureResponse(serviceResult, null, Locale.getDefault());
            }

            log.info("< Final response: {}", finalResponse);

            return ResponseEntity.ok(finalResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error during logon for user {}: {}", request.getData().username(), e.getMessage());
            finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("unknown.unable.to.process.code", e.getMessage())
                    , null
                    , Locale.getDefault());
//            return ResponseEntity.badRequest().build();
            return ResponseEntity.badRequest().body(finalResponse);

        } catch (GatewayIntegrationException e) {
            log.error("Gateway error during logon for user {}: Code={}, Desc={}, Info={}", request.getData().username(), e.getCode(), e.getDescription(), e.getInfo());

            finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure(e.getCode(), e.getMessage())
                    , null
                    , Locale.getDefault());

            return ResponseEntity.badRequest().body(finalResponse);
            // Return 502 Bad Gateway for upstream errors
//            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();

        } catch (RuntimeException e) {
            log.error("Unexpected error during logon for user {}: {}", request.getData().username(), e.getMessage());

            finalResponse = responseBuilder.buildFailureResponse(
                    ServiceResult.failure("", e.getMessage())
                    , null
                    , Locale.getDefault());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(finalResponse);
        } finally {
            log.info("<<< END logon request <<<");
        }
    }
}
