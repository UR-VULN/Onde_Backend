package com.onde.core.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final ErrorInfo error;
    private final String timestamp;

    private ErrorResponse(String message, String errorCode, String systemMessage, List<ErrorDetail> details) {
        this.message = message;
        this.error = new ErrorInfo(errorCode, systemMessage, details);
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Getter
    public static class ErrorInfo {
        private final String code;
        private final List<ErrorDetail> details;

        // systemMessage는 서버 내부 로깅 전용으로만 사용 — 클라이언트에 노출하지 않음
        @JsonIgnore
        private final String systemMessage;

        public ErrorInfo(String code, String systemMessage, List<ErrorDetail> details) {
            this.code = code;
            this.systemMessage = systemMessage;
            this.details = details;
        }
    }

    // [Overloading 1] 단순 비즈니스 예외 처리용
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), null, null);
    }

    // [Overloading 2] 시스템 예외 메시지 내부 기록용 (클라이언트에는 미노출)
    public static ErrorResponse of(ErrorCode errorCode, String systemMessage) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), systemMessage, null);
    }

    // [Overloading 3] @Valid 유효성 검증 실패용
    public static ErrorResponse of(ErrorCode errorCode, String systemMessage, List<ErrorDetail> details) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.getCode(), systemMessage, details);
    }

    // [Overloading 4] 유저 노출 메시지 커스텀 튜닝용
    public static ErrorResponse of(ErrorCode errorCode, String customUserMessage, String systemMessage,
            List<ErrorDetail> details) {
        return new ErrorResponse(customUserMessage, errorCode.getCode(), systemMessage, details);
    }
}
