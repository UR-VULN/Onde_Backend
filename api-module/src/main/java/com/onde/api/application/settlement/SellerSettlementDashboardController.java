package com.onde.api.application.settlement;

import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
public class SellerSettlementDashboardController {

    private final SettlementService settlementService;

    /**
     * 판매자 대시보드 매출 통계 조회 API
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<com.onde.api.application.settlement.dto.SellerDashboardResponse>> getSellerDashboardStatistics(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(name = "period", defaultValue = "MONTHLY") String period,
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {

        com.onde.api.application.payment.dto.response.SellerStatisticsResponse stats = settlementService.getSellerStatistics(sellerId);
        
        java.util.List<com.onde.api.application.settlement.dto.SellerDashboardResponse.RevenueBreakdown> breakdown = new java.util.ArrayList<>();
        for (com.onde.api.application.payment.dto.response.SellerStatisticsResponse.RevenueTrend trend : stats.getMonthlyTrends()) {
            breakdown.add(com.onde.api.application.settlement.dto.SellerDashboardResponse.RevenueBreakdown.builder()
                    .month(trend.getLabel())
                    .revenue(trend.getGrossAmount())
                    .bookingCount(1) // 테스트용 가상 카운트
                    .build());
        }

        com.onde.api.application.settlement.dto.SellerDashboardResponse response = com.onde.api.application.settlement.dto.SellerDashboardResponse.builder()
                .period(period)
                .totalRevenue(stats.getMonthlyTrends().stream().mapToLong(t -> t.getGrossAmount()).sum())
                .breakdown(breakdown)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
