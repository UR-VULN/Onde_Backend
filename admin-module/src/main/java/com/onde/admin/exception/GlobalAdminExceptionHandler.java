package com.onde.admin.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorDetail;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalAdminExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("Admin business exception occurred: {}", e.getMessage(), e);
        HttpStatus status;
        
        if (e.getClass().getSimpleName().contains("NotFound")) {
            status = HttpStatus.NOT_FOUND;
        } else if (e.getClass().getSimpleName().contains("Validation")) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        ErrorResponse response = ErrorResponse.of(
                e.getErrorCode(),
                e.getMessage()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Admin validation exception occurred: {}", e.getMessage(), e);
        
        List<ErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.add(new ErrorDetail(
                    fieldError.getField(),
                    fieldError.getRejectedValue() == null ? "null" : fieldError.getRejectedValue().toString(),
                    fieldError.getDefaultMessage()
            ));
        }

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                "어드민 입력값이 올바르지 않습니다.",
                e.getClass().getSimpleName() + ": Admin Validation failed for object='" + e.getBindingResult().getObjectName() + "'",
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled admin server exception occurred: {}", e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "어드민 서버 내부 오류가 발생했습니다.",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

