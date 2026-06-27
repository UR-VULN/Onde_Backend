package com.onde.admin.application.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 본사 관리자(SELLER_ADMIN/SUPER_ADMIN)를 위한 정산 상세 내역 응답 DTO 클래스입니다.
 */
@Getter
@Builder
public class AdminSettlementDetailResponse {

    private String settlementId;
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
