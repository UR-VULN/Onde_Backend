package com.onde.api.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorDetail;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * 1. 비즈니스 요구사항에 정의된 커스텀 예외 처리
         * ErrorResponse.of의 4개 인자 스펙에 완벽 대응
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
                log.error("🔒 [BusinessException] 발생: {} | Message: {}", e.getErrorCode().getCode(), e.getMessage(), e);

                ErrorCode errorCode = e.getErrorCode();
                String systemMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();

                // 👈 순정 ErrorResponse.of(message, code, systemMessage, details) 스펙 준수
                ErrorResponse response = ErrorResponse.of(
                                errorCode.getMessage(), // 1. String message
                                errorCode.getCode(), // 2. String code
                                systemMessage, // 3. String systemMessage
                                null // 4. List<ErrorDetail> details (비즈니스 예외는 하부 필드가 없으므로 null)
                );

                return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
        }

        /**
         * 2. @Valid, @Validated 변수 검증(Validation) 실패 예외 처리
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
                log.warn("⚠️ [ValidationException] 데이터 검증 실패: {}", e.getMessage(), e);

                List<ErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                                .map(fieldError -> new ErrorDetail(
                                                fieldError.getField(),
                                                fieldError.getRejectedValue() == null ? "null"
                                                                : fieldError.getRejectedValue().toString(),
                                                fieldError.getDefaultMessage()))
                                .collect(Collectors.toList());

                String defaultMessage = "입력값이 올바르지 않습니다.";
                if (!e.getBindingResult().getAllErrors().isEmpty()) {
                        defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
                }

                // 👈 순정 ErrorResponse.of(message, code, systemMessage, details) 스펙 준수
                ErrorResponse response = ErrorResponse.of(
                                defaultMessage, // 1. String message
                                ErrorCode.INVALID_INPUT_VALUE.getCode(), // 2. String code
                                "Validation failed for object='" + e.getBindingResult().getObjectName() + "'", // 3.
                                                                                                               // String
                                                                                                               // systemMessage
                                details // 4. List<ErrorDetail> details
                );

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        /**
         * 3. 시스템 최상위 예외 (500 Internal Server Error 방어선)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleException(Exception e) {
                log.error("🚨 [Unhandled Exception] 예측하지 못한 시스템 최상위 에러 감지: ", e);

                String systemMessage = String.format("%s: %s", e.getClass().getName(),
                                e.getMessage() != null ? e.getMessage() : "No detailed message");

                // 👈 순정 ErrorResponse.of(message, code, systemMessage, details) 스펙 준수
                ErrorResponse response = ErrorResponse.of(
                                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.", // 1. String message
                                ErrorCode.INTERNAL_SERVER_ERROR.getCode(), // 2. String code
                                systemMessage, // 3. String systemMessage
                                null // 4. List<ErrorDetail> details
                );

                return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(response);
        }
}