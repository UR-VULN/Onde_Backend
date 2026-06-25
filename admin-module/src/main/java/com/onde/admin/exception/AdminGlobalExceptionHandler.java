package com.onde.admin.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException; 
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException; 
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
// 관리자 컨트롤러에서 터진 에러
@RestControllerAdvice(basePackages = "com.onde.admin")
public class AdminGlobalExceptionHandler {

    // 로그인 실패 처리
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Admin Login Failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "아이디 또는 비밀번호가 일치하지 않습니다.", null, null));
    }

    // 계정 잠금 처리 
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("Admin Account Locked: {}", e.getMessage());
        HttpStatus status = e.getMessage().contains("잠겼습니다") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage(), null, null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Admin BusinessException: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        String userMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, userMessage, null, null)); // systemMessage 차단
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Admin ValidationException: {}", e.getMessage());
        String defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(ErrorCode.INVALID_COORDINATE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_COORDINATE, defaultMessage, null, null));
    }

    // JSON 파괴 공격 시 기술 스택 노출 차단
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Admin HttpMessageNotReadableException (잘못된 요청 형식): {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "잘못된 요청 형식입니다. 입력값을 확인해주세요.", null, null));
    }

    // 404 에러 (존재하지 않는 API 경로 요청 시 방어)
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
        log.warn("Admin NotFoundException (없는 경로 요청): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청하신 API 경로를 찾을 수 없습니다.", null, null));
    }

    // 405 에러 (지원하지 않는 HTTP 메서드 요청 시 방어)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Admin HttpRequestMethodNotSupportedException (잘못된 메서드): {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 HTTP 메서드 요청입니다.", null, null));
    }

    // 미디어 타입 방어 (빈 바디 등으로 인한 프레임워크 에러)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("Admin HttpMediaTypeNotSupportedException (잘못된 Content-Type): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 데이터 형식입니다.", null, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Admin AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, "접근 권한이 없습니다.", null, null));
    }

    // 최상위 에러 발생 시 내부 구조 노출 원천 차단
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Admin Unhandled Exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "어드민 서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", null, null));
    }
}