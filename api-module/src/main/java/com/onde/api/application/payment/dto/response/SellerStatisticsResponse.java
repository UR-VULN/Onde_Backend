package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * 판매자용 일별/월별 누적 매출 추이 통계 데이터를 반환하는 DTO 클래스입니다.
 */
@Getter
@Builder
public class SellerStatisticsResponse {
    
    /**
     * 통계 대상 판매자 식별자 (PK)
     */
    private Long sellerId;

    /**
     * 일별 매출 및 누적 추이 리스트
     */
    private List<RevenueTrend> dailyTrends;

    /**
     * 월별 매출 및 누적 추이 리스트
     */
    private List<RevenueTrend> monthlyTrends;

    /**
     * 정산 금액 추이를 나타내는 중첩 클래스입니다.
     */
    @Getter
    @Builder
    public static class RevenueTrend {
        /**
         * 기준 레이블 (일별: "yyyy-MM-dd", 월별: "yyyy-MM")
         */
        private String label;

        /**
         * 해당 기간(일/월) 동안의 총 매출액
         */
        private Long grossAmount;

        /**
         * 해당 기간(일/월) 동안의 수수료 차감 후 판매자 실수령 정산금
         */
        private Long netAmount;

        /**
         * 전체 집계 대상 기간의 누적 총 매출액
         */
        private Long accumulatedGrossAmount;

        /**
         * 전체 집계 대상 기간의 누적 순 정산금
         */
        private Long accumulatedNetAmount;
    }
}
