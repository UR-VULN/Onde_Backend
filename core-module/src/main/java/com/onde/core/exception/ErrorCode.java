package com.onde.core.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Member 관련 오류
    MEMBER_NOT_FOUND("M001", "회원 정보를 찾을 수 없습니다."),

    // Flight 관련 오류
    FLIGHT_SCHEDULE_NOT_FOUND("F001", "요청하신 항공 스케줄을 찾을 수 없습니다."),
    SEAT_INVENTORY_NOT_FOUND("F002", "요청하신 좌석 등급의 재고 정보를 찾을 수 없습니다."),
    SEAT_SOLD_OUT("F003", "선택하신 좌석이 모두 매진되었습니다."),
    BOOKING_NOT_FOUND("F004", "해당 항공 예약을 찾을 수 없습니다."),

    // Insurance 관련 오류
    INSURANCE_PRODUCT_NOT_FOUND("I001", "요청하신 여행자 보험 상품을 찾을 수 없습니다."),
    INSURANCE_POLICY_NOT_FOUND("I002", "요청하신 보험 가입 정보 계약을 찾을 수 없습니다."),

    // 공통 오류
    INVALID_COORDINATE("C001", "위경도 좌표는 소수점 4자리 이상의 정밀도를 지녀야 합니다."),
    INVALID_INPUT_VALUE("C002", "입력하신 값이 유효하지 않습니다."),
    POST_NOT_FOUND("C003", "요청하신 커뮤니티 게시글을 찾을 수 없습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
