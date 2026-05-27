package com.onde.core.support;

import lombok.Getter;
import java.util.List;

@Getter
public class ErrorDetail {
    private final String code;
    private final String systemMessage;
    private final List<ValidationErrorDetail> details;

    public ErrorDetail(String code, String systemMessage, List<ValidationErrorDetail> details) {
        this.code = code;
        this.systemMessage = systemMessage;
        this.details = details;
    }

    @Getter
    public static class ValidationErrorDetail {
        private final String field;
        private final Object rejectedValue;
        private final String reason;

        public ValidationErrorDetail(String field, Object rejectedValue, String reason) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.reason = reason;
        }
    }
}
