package com.onde.api.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.onde.api")
public class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
                log.error("🔒 [BusinessException] 발생: {} | Message: {}", e.getErrorCode().getCode(), e.getMessage(), e);
                ErrorCode errorCode = e.getErrorCode();
                String userMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
                return ResponseEntity.status(errorCode.getHttpStatus())
                        .body(ErrorResponse.of(errorCode, userMessage, null, null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
                log.warn("⚠️ [ValidationException] 데이터 검증 실패");
                String defaultMessage = "입력값이 올바르지 않습니다.";
                if (!e.getBindingResult().getAllErrors().isEmpty()) {
                        defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
                }
                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, defaultMessage, null, null));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
                log.warn("⚠️ [HttpMessageNotReadableException] 잘못된 요청 바디 형식");
                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "잘못된 요청 형식입니다. 입력값을 확인해주세요.", null, null));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleException(Exception e) {
                log.error("🚨 [Unhandled Exception] 예측하지 못한 시스템 최상위 에러 감지: ", e);
                // 내부 클래스 이름 조합 로직(String.format...) 완전 삭제 및 null 처리
                return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                        .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.", null, null));
        }
}