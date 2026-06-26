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
    private final ErrorInfo error; // ErrorInfo 객체로 선언
    private final String timestamp;

    // 내부 생성자
    private ErrorResponse(String message, String errorCode, String systemMessage, List<ErrorDetail> details) {
        this.message = message;
        this.error = new ErrorInfo(errorCode, systemMessage, details);
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * 에러의 핵심 정보를 담는 내부 정적 클래스 (ErrorInfo)
     * 이를 분리해 주어야 ErrorDetail 생성자가 안 깨집니다.
     */
    @Getter
    public static class ErrorInfo {
        private final String code;
        private final String systemMessage;
        private final List<ErrorDetail> details;

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

    // [Overloading 2] 시스템 예외 메시지 추가용
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

    /**
     * 클라이언트 응답용 — 오류 코드·시스템 메시지·입력값(rejectedValue)을 노출하지 않습니다.
     */
    public static ErrorResponse client(String message) {
        return new ErrorResponse(message, null, null, null);
    }
}