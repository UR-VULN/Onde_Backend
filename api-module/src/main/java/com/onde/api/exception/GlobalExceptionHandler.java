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
                                details
                );

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(SecurityException.class)
        public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
            log.warn("🚨 [SecurityException] 보안 정책 위반: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.of(
                    ErrorCode.INVALID_INPUT_VALUE,
                    e.getMessage(),
                    "Security policy violation",
                    null
            );
            return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolationException(
                jakarta.validation.ConstraintViolationException e) {
            String message = e.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));
            ErrorResponse response = ErrorResponse.of(
                    ErrorCode.INVALID_INPUT_VALUE,
                    message,
                    "Constraint violation",
                    null
            );
            return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
                log.warn("⚠️ [IllegalArgumentException] 잘못된 요청: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());

                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
                org.springframework.http.converter.HttpMessageNotReadableException e) {
                log.warn("⚠️ [HttpMessageNotReadableException] 요청 바디 파싱 실패: {}", e.getMessage());
                ErrorResponse response = ErrorResponse.of(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "잘못된 HTTP 요청 바디 형식입니다.",
                        "Http message not readable",
                        null
                );
                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
        public ResponseEntity<ErrorResponse> handleMultipartException(org.springframework.web.multipart.MultipartException e) {
                log.warn("⚠️ [MultipartException] 멀티파트 파일 업로드 실패: {}", e.getMessage());
                ErrorResponse response = ErrorResponse.of(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "파일 업로드 요청 처리에 실패했습니다. 파일 크기 제한(개별 10MB, 전체 30MB) 또는 전송 스트림 형식을 확인하세요.",
                        "Multipart parsing failed",
                        null
                );
                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
                log.warn("🚫 [AccessDeniedException] 권한 없는 요청: {}", e.getMessage());

                ErrorResponse response = ErrorResponse.of(ErrorCode.FORBIDDEN, e.getMessage());

                return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(response);
        }

        @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
                log.warn("⚠️ [TypeMismatchException] 파라미터 타입 불일치: {}", e.getMessage());
                
                ErrorResponse response = ErrorResponse.of(
                                ErrorCode.INVALID_INPUT_VALUE,
                                "잘못된 파라미터 형식입니다.",
                                "Type mismatch for parameter: " + e.getName(),
                                null
                );
                return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(response);
        }

        /**
         * 3. 시스템 최상위 예외 (500 Internal Server Error 방어선)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleException(Exception e) {
                log.error("🚨 [Unhandled Exception] 예측하지 못한 시스템 최상위 에러 감지: ", e);

                // [보안 강화 - WEB-11] 500 에러 응답 시 클래스 물리명 및 내부 에러 메시지가 클라이언트에 노출되지 않도록 가림
                ErrorResponse response = ErrorResponse.of(
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.",
                                "Internal Server Error",
                                null
                );

                return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(response);
        }
}
