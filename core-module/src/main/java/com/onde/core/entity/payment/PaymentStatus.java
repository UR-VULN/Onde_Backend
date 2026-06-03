package com.onde.core.entity.payment;

/**
 * 결제 진행 상태를 나타내는 열거형(Enum)입니다.
 */
public enum PaymentStatus {
    /**
     * PENDING: 결제 요청 대기 중 (PG 사 승인 전)
     */
    PENDING,

    /**
     * PAID: 결제 완료 (PG 사 승인 및 마일리지 처리 완료)
     */
    PAID,

    /**
     * CANCELLED: 결제 승인 실패 또는 취소
     */
    CANCELLED,

    /**
     * REFUNDED: 환불 완료 (결제 완료 후 예약 취소에 따른 전체 / 부분 환불)
     */
    REFUNDED
}

