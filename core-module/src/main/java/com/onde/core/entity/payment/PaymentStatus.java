package com.onde.core.entity.payment;

/**
 * 결제 진행 상태를 나타내는 열거형(Enum)입니다.
 */
public enum PaymentStatus {
    /** 결제 대기 */
    PENDING,

    /** 결제 완료 */
    PAID,

    /** 결제 취소 */
    CANCELLED,

    /** 환불 완료 */
    REFUNDED
}

