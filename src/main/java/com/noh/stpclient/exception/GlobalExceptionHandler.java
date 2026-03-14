package com.noh.stpclient.exception;

import com.noh.stpclient.model.EResponseStatus;
import com.noh.stpclient.model.base.BaseResponse;
import com.noh.stpclient.model.base.ErrorDetails;
import com.noh.stpclient.model.base.ErrorResponse;
import com.noh.stpclient.model.base.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode("VAL_001")
                .errorMessage("Validation failed for one or more fields")
                .errorCategory("VALIDATION")
                .fieldErrors(fieldErrors)
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("400")
                .resMessage("Bad Request")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request body uri={} error={}", request.getRequestURI(), ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode("VAL_002")
                .errorMessage("Malformed or unreadable request body")
                .errorCategory("VALIDATION")
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("400")
                .resMessage("Bad Request")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode("NOT_FOUND")
                .errorMessage("No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL())
                .errorCategory("TECHNICAL")
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("404")
                .resMessage("Not Found")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(GatewayIntegrationException.class)
    public ResponseEntity<BaseResponse<?>> handleGatewayIntegrationException(
            GatewayIntegrationException ex,
            HttpServletRequest request) {

        log.error("Gateway error uri={} code={} description={} info={}",
                request.getRequestURI(), ex.getCode(), ex.getDescription(), ex.getInfo());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(ex.getCode())
                .errorMessage(ex.getDescription())
                .errorCategory("BUSINESS")
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("502")
                .resMessage("Bad Gateway")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception uri={}", request.getRequestURI(), ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode("INTERNAL_ERROR")
                .errorMessage("An unexpected error occurred")
                .errorCategory("TECHNICAL")
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("500")
                .resMessage("Internal Server Error")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
