package com.onde.core.support;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@NoArgsConstructor
public class ErrorResponse {
    private final boolean success = false;
    private final Object data = null;
    private String message;
    private ErrorInfo error;
    private String timestamp;

    @Getter
    @NoArgsConstructor
    public static class ErrorInfo {
        private String code;
        private String systemMessage;
        private List<ErrorDetail> details;

        public ErrorInfo(String code, String systemMessage, List<ErrorDetail> details) {
            this.code = code;
            this.systemMessage = systemMessage;
            this.details = details;
        }
    }

    public ErrorResponse(String message, ErrorInfo error) {
        this.message = message;
        this.error = error;
        this.timestamp = Instant.now().toString();
    }

    public static ErrorResponse of(String message, String code, String systemMessage, List<ErrorDetail> details) {
        return new ErrorResponse(message, new ErrorInfo(code, systemMessage, details));
    }
}


