package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 어드민용 전사 총거래액(GMV), 수수료 이익, 서비스별 매출 비중 통계 데이터를 반환하는 DTO 클래스입니다.
 */
@Getter
@Builder
public class PlatformStatisticsResponse {

    /**
     * 플랫폼 전체 총 거래액 (GMV)
     */
    private BigDecimal totalGmv;

    /**
     * 플랫폼 전체 수수료 순이익
     */
    private BigDecimal totalCommission;

    /**
     * 서비스 유형별 매출 비중 목록
     */
    private List<ServiceRevenueShare> serviceShares;

    /**
     * 서비스별 매출 및 비중 정보를 담은 중첩 클래스입니다.
     */
    @Getter
    @Builder
    public static class ServiceRevenueShare {
        /**
         * 서비스 유형 (예: ROOM, CAR, FLIGHT, INSURANCE 등)
         */
        private String serviceType;

        /**
         * 해당 서비스의 총거래액
         */
        private BigDecimal grossAmount;

        /**
         * 전체 매출 대비 해당 서비스의 비중 (0.0 ~ 1.0 비율)
         */
        private Double shareRate;
    }
}
