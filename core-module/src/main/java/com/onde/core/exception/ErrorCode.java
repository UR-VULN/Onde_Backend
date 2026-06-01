package com.onde.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter // 👈 롬복 애노테이션으로 하단의 게터 메서드들을 깔끔하게 대체합니다.
public enum ErrorCode {

    // =========================================================================
    // M: Member 관련 오류
    // =========================================================================
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M-001", "회원 정보를 찾을 수 없습니다."),

    // =========================================================================
    // F: Flight (항공) 관련 오류
    // =========================================================================
    FLIGHT_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "F-001", "요청하신 항공 스케줄을 찾을 수 없습니다."),
    SEAT_INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "F-002", "요청하신 좌석 등급의 재고 정보를 찾을 수 없습니다."),
    SEAT_SOLD_OUT(HttpStatus.BAD_REQUEST, "F-003", "선택하신 좌석이 모두 매진되었습니다."),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "F-004", "해당 항공 예약을 찾을 수 없습니다."),

    // =========================================================================
    // I: Insurance (보험) 관련 오류
    // =========================================================================
    INSURANCE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "I-001", "요청하신 여행자 보험 상품을 찾을 수 없습니다."),
    INSURANCE_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "I-002", "요청하신 보험 가입 정보 계약을 찾을 수 없습니다."),

    // =========================================================================
    // R: Reservation (공통 예약) 관련 오류
    // =========================================================================
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R-001", "예약을 찾을 수 없습니다."),
    RESERVATION_NOT_OWNER(HttpStatus.FORBIDDEN, "R-002", "본인 예약 건만 작업 가능합니다."),

    // =========================================================================
    // C: Community (커뮤니티/공통) 관련 오류
    // =========================================================================
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "C-001", "위경도 좌표는 소수점 4자리 이상의 정밀도를 지녀야 합니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C-002", "입력하신 값이 유효하지 않습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "C-003", "이미지는 최대 3장까지 첨부할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "C-004", "요청하신 커뮤니티 게시글을 찾을 수 없습니다."),
    POST_NOT_OWNER(HttpStatus.FORBIDDEN, "C-005", "본인 게시글만 작업할 수 있습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C-006", "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "C-007", "본인 댓글만 작업할 수 있습니다."),

    // =========================================================================
    // A: Auth/Security 관련 오류
    // =========================================================================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "인증에 실패하였습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-002", "해당 API에 접근할 권한이 없습니다."),

    // =========================================================================
    // S: Server 관련 오류
    // =========================================================================
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-001", "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}