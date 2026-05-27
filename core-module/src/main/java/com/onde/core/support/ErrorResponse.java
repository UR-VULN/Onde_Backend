package com.onde.core.support;

import com.onde.core.exception.ErrorCode;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class ErrorResponse {
    private final boolean success = false;
    private final Object data = null;
    private final String message;
    private final ErrorDetail error;
    private final String timestamp;

    private ErrorResponse(String message, String errorCode, String systemMessage, List<ErrorDetail.ValidationErrorDetail> details) {
        this.message = message;
        this.error = new ErrorDetail(errorCode, systemMessage, details);
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), null, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String systemMessage) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), systemMessage, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String systemMessage, List<ErrorDetail.ValidationErrorDetail> details) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), systemMessage, details);
    }

    public static ErrorResponse of(ErrorCode errorCode, String customUserMessage, String systemMessage, List<ErrorDetail.ValidationErrorDetail> details) {
        return new ErrorResponse(customUserMessage, errorCode.getCode(), systemMessage, details);
    }
}
