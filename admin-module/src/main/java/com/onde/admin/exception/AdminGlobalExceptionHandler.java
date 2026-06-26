package com.onde.admin.exception;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.RestExceptionHandlerSupport;
import com.onde.core.support.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class AdminGlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Admin BusinessException: {}", e.getMessage(), e);
        return RestExceptionHandlerSupport.business(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Admin ValidationException: {}", e.getMessage());
        return RestExceptionHandlerSupport.validation(e);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("Admin BindException: {}", e.getMessage());
        return RestExceptionHandlerSupport.bind(e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Admin ConstraintViolationException: {}", e.getMessage());
        return RestExceptionHandlerSupport.constraintViolation(e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Admin HttpMessageNotReadableException: {}", e.getMessage());
        return RestExceptionHandlerSupport.notReadable(e);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Admin MethodArgumentTypeMismatchException: param={}", e.getName());
        return RestExceptionHandlerSupport.typeMismatch(e);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Admin MissingServletRequestParameterException: param={}", e.getParameterName());
        return RestExceptionHandlerSupport.missingParameter(e);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("Admin MaxUploadSizeExceededException: {}", e.getMessage());
        return RestExceptionHandlerSupport.uploadSizeExceeded(e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("Admin AccessDeniedException: {}", e.getMessage());
        return RestExceptionHandlerSupport.accessDenied(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Admin Unhandled Exception", e);
        return RestExceptionHandlerSupport.internalServerError();
    }
}
