package com.onde.core.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M-001", "회원을 찾을 수 없습니다."),
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "L-001", "좌표는 소수점 4자리 이상 정밀도여야 합니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "C-001", "이미지는 최대 3장까지 첨부할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "C-002", "게시글을 찾을 수 없습니다."),
    POST_NOT_OWNER(HttpStatus.FORBIDDEN, "C-003", "본인 게시글만 작업할 수 있습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C-004", "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "C-005", "본인 댓글만 작업할 수 있습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R-001", "예약을 찾을 수 없습니다."),
    RESERVATION_NOT_OWNER(HttpStatus.FORBIDDEN, "R-002", "본인 예약 건만 다운로드 가능합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-001", "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
