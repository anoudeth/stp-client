package com.noh.stpclient.exception;

import com.noh.stpclient.model.EResponseStatus;
import com.noh.stpclient.model.base.BaseResponse;
import com.noh.stpclient.model.base.ErrorDetails;
import com.noh.stpclient.model.base.ErrorResponse;
import com.noh.stpclient.model.base.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
//                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
//                        .code("VALIDATION_ERROR")
                        .build())
                .collect(Collectors.toList());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode("VAL_001")
//                .errorMessage("Validation failed for one or more fields")
//                .errorCategory("VALIDATION")
                .fieldErrors(fieldErrors)
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .resCode("400")
                .resMessage("Bad Request")
                .resStatus(EResponseStatus.FAILED)
                .resTimestamp(Instant.now())
                .processingTime(0L)
                .error(errorDetails)  // Need to uncomment this field in BaseResponse
                .build();

        return ResponseEntity.badRequest().body(response);
    }
}
