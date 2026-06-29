package com.onde.core.entity.payment;

/**
 * 마일리지 변동 유형을 정의하는 열거형(Enum)입니다.
 */
public enum MileageLogType {
    /**
     * EARN: 마일리지 적립 - 결제 완료 후 금액의 일정 비율을 적립할 때 사용
     */
    EARN,

    /**
     * USE: 마일리지 사용 - 복합 결제 시 마일리지로 일부 금액을 차감할 때 사용
     */
    USE,

    /**
     * RESTORE: 마일리지 복구 - 예약 취소/환불 시 기존에 사용했던 마일리지를 다시 돌려줄 때 사용
     */
    RESTORE,

    /**
     * REVOKE: 마일리지 회수 - 예약 취소/환불 시 적립되었던 마일리지를 다시 차감할 때 사용
     */
    REVOKE
}

