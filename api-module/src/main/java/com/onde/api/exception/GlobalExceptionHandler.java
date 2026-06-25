package com.onde.api.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorDetail;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
                String userMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();

                ErrorResponse response = ErrorResponse.of(errorCode, userMessage, userMessage, null);

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

                ErrorResponse response = ErrorResponse.of(
                                ErrorCode.INVALID_INPUT_VALUE,
                                defaultMessage,
                                "Validation failed for object='" + e.getBindingResult().getObjectName() + "'",
                                details);

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler({
                        org.springframework.http.converter.HttpMessageNotReadableException.class,
                        org.springframework.http.converter.HttpMessageConversionException.class,
                        com.fasterxml.jackson.core.JsonProcessingException.class
        })
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(Exception e) {
                log.warn("⚠️ [HttpMessageNotReadableException] 잘못된 JSON 요청 데이터: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                ErrorCode.INVALID_INPUT_VALUE,
                                "요청 데이터의 형식이 올바르지 않거나 중복된 키가 존재합니다.",
                                null,
                                null);

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
                log.warn("⚠️ [IllegalArgumentException] 잘못된 요청: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "입력값이 유효하지 않습니다.");

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(java.util.NoSuchElementException.class)
        public ResponseEntity<ErrorResponse> handleNoSuchElementException(java.util.NoSuchElementException e) {
                log.warn("⚠️ [NoSuchElementException] 리소스 없음: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다.", null, null);

                return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus()).body(response);
        }

        @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatchException(org.springframework.beans.TypeMismatchException e) {
                log.warn("⚠️ [TypeMismatchException] 잘못된 데이터 형식: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청 형식이 올바르지 않습니다.");

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
                log.warn("🚫 [AccessDeniedException] 권한 없는 요청: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.FORBIDDEN, e.getMessage());

                return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(response);
        }

        /**
         * 3. 시스템 최상위 예외 (500 Internal Server Error 방어선)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleException(Exception e) {
                log.error("🚨 [Unhandled Exception] 예측하지 못한 시스템 최상위 에러 감지: ", e);

                ErrorResponse response = ErrorResponse.of(
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.",
                                e.toString() + " - " + e.getMessage(),
                                null);

                return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(response);
        }
}
