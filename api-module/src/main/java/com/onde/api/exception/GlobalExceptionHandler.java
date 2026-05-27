package com.onde.api.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorDetail;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        
        // systemMessage로 비즈니스 예외 로그 또는 detail 메시지 세팅
        String systemMessage = e.getMessage(); 
        
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, systemMessage));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("ValidationException: {}", e.getMessage(), e);
        
        // 모든 필드 에러를 표준 에러 Envelope 포맷에 맞춰 수집
        List<ErrorDetail.ValidationErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorDetail.ValidationErrorDetail(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .collect(Collectors.toList());
        
        String defaultMessage = "입력값이 올바르지 않습니다.";
        if (!e.getBindingResult().getAllErrors().isEmpty()) {
            defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        return ResponseEntity
                .status(ErrorCode.INVALID_COORDINATE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_COORDINATE, defaultMessage, e.getClass().getName(), details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception", e);
        
        // systemMessage로 시스템 예외 타입과 메시지 수집
        String systemMessage = String.format("%s: %s", e.getClass().getName(), e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, systemMessage));
    }
}
