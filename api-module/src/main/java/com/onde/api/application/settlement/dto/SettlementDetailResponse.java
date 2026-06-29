package com.onde.api.application.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 상세 정보(개별 예약 결제 건들)를 반환하기 위한 응답 DTO 클래스입니다.
 */
@Getter
@Builder
public class SettlementDetailResponse {

    private Long settlementId;
    private LocalDate settlementDate;
    private List<DetailItem> details;

    @Getter
    @Builder
    public static class DetailItem {
        private Long paymentId;
        private Long reservationId;
        private String targetType;
        private String productName;
        private BigDecimal amount;
        private LocalDateTime paymentDate;
    }
}
