package com.onde.core.entity.settlement;

/**
 * 정산 진행 상태를 나타내는 열거형(Enum)입니다.
 * 정산은 배치 처리에 의해 생성된 후, 판매자의 신청 및 다단계(본사 담당자 -> 최고 관리자) 승인을 거쳐 최종 COMPLETED(완료) 처리됩니다.
 */
public enum SettlementStatus {
    /**
     * 배치에 의해 자동 생성된 최초 상태. 아직 지급 신청되지 않은 상태입니다.
     */
    PENDING,

    /**
     * 판매자가 확인 후 정산금 지급을 신청한 상태입니다.
     */
    REQUESTED,

    /**
     * 본사의 1차 정산 담당자(SALES_ADMIN 등)가 금액 검증 및 서류 검토 후 1차 승인한 상태입니다.
     */
    APPROVED_1ST,

    /**
     * 본사의 최고 관리자(SUPER_ADMIN)가 최종적으로 지급 확정(이체 등)을 완료한 상태입니다.
     */
    COMPLETED
}

