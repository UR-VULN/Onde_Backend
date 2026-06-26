package com.onde.core.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.onde.core.support.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

public final class RestExceptionHandlerSupport {

    private RestExceptionHandlerSupport() {
    }

    public static ResponseEntity<ErrorResponse> business(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        String userMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
        return ResponseEntity.status(errorCode.getHttpStatus().value()).body(ErrorResponse.client(userMessage));
    }

    public static ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        String message = resolveFirstValidationMessage(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.client(message));
    }

    public static ResponseEntity<ErrorResponse> bind(BindException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력 형식이 올바르지 않습니다.")
                .orElse("입력 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.client(message));
    }

    public static ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("입력 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.client(message));
    }

    public static ResponseEntity<ErrorResponse> notReadable(HttpMessageNotReadableException e) {
        String message = "요청 형식이 올바르지 않습니다.";
        Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException) {
            message = "허용되지 않은 요청 필드가 포함되어 있습니다.";
        } else if (cause instanceof InvalidFormatException ife) {
            message = resolveInvalidFormatMessage(ife);
        } else if (cause instanceof MismatchedInputException mie) {
            message = resolveMismatchedInputMessage(mie);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.client(message));
    }

    public static ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName() != null ? e.getName() : "parameter";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.client("'" + paramName + "' 파라미터 형식이 올바르지 않습니다."));
    }

    public static ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.client("필수 파라미터 '" + e.getParameterName() + "'가 누락되었습니다."));
    }

    public static ResponseEntity<ErrorResponse> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.client("첨부 파일 크기는 10MB를 초과할 수 없습니다."));
    }

    public static ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException e) {
        String message = ClientSafeErrorMessage.fromIllegalArgument(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.client(message));
    }

    public static ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus().value())
                .body(ErrorResponse.client(ErrorCode.FORBIDDEN.getMessage()));
    }

    public static ResponseEntity<ErrorResponse> internalServerError() {
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value())
                .body(ErrorResponse.client("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private static String resolveFirstValidationMessage(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }
        return e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력 형식이 올바르지 않습니다.")
                .orElse("입력 형식이 올바르지 않습니다.");
    }

    private static String resolveInvalidFormatMessage(InvalidFormatException ife) {
        if (!ife.getPath().isEmpty() && ife.getPath().get(0).getFieldName() != null) {
            return "'" + ife.getPath().get(0).getFieldName() + "' 필드 형식이 올바르지 않습니다.";
        }
        return "요청 필드 형식이 올바르지 않습니다.";
    }

    private static String resolveMismatchedInputMessage(MismatchedInputException mie) {
        if (!mie.getPath().isEmpty() && mie.getPath().get(0).getFieldName() != null) {
            return "'" + mie.getPath().get(0).getFieldName() + "' 필드 형식이 올바르지 않습니다.";
        }
        return "요청 형식이 올바르지 않습니다.";
    }
}
