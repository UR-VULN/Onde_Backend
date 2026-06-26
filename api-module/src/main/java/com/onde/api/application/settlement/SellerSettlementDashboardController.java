package com.onde.api.application.settlement;

import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
public class SellerSettlementDashboardController {

    private final SettlementService settlementService;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<com.onde.api.application.settlement.dto.SellerDashboardResponse>> getSellerDashboardStatistics(
            @LoginMember Long sellerId,
            @RequestParam(name = "period", defaultValue = "MONTHLY")
            @Pattern(regexp = "^(MONTHLY|WEEKLY|DAILY)$", message = "period는 MONTHLY, WEEKLY, DAILY만 허용됩니다.")
            String period,
            @RequestParam(name = "startDate")
            @NotBlank(message = "startDate는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDate는 YYYY-MM-DD 형식이어야 합니다.")
            String startDate,
            @RequestParam(name = "endDate")
            @NotBlank(message = "endDate는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "endDate는 YYYY-MM-DD 형식이어야 합니다.")
            String endDate) {

        com.onde.api.application.payment.dto.response.SellerStatisticsResponse stats = settlementService.getSellerStatistics(sellerId);

        java.util.List<com.onde.api.application.settlement.dto.SellerDashboardResponse.RevenueBreakdown> breakdown = new java.util.ArrayList<>();
        for (com.onde.api.application.payment.dto.response.SellerStatisticsResponse.RevenueTrend trend : stats.getMonthlyTrends()) {
            breakdown.add(com.onde.api.application.settlement.dto.SellerDashboardResponse.RevenueBreakdown.builder()
                    .month(trend.getLabel())
                    .revenue(trend.getGrossAmount())
                    .bookingCount(1)
                    .build());
        }

        java.util.List<BigDecimal> dailyRevenue = new java.util.ArrayList<>();
        for (com.onde.api.application.payment.dto.response.SellerStatisticsResponse.RevenueTrend trend : stats.getDailyTrends()) {
            dailyRevenue.add(trend.getGrossAmount());
        }
        while (dailyRevenue.size() < 7) {
            dailyRevenue.add(BigDecimal.ZERO);
        }
        if (dailyRevenue.size() > 7) {
            dailyRevenue = dailyRevenue.subList(dailyRevenue.size() - 7, dailyRevenue.size());
        }

        com.onde.api.application.settlement.dto.SellerDashboardResponse response = com.onde.api.application.settlement.dto.SellerDashboardResponse.builder()
                .period(period)
                .totalRevenue(stats.getMonthlyTrends().stream()
                        .map(com.onde.api.application.payment.dto.response.SellerStatisticsResponse.RevenueTrend::getGrossAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .breakdown(breakdown)
                .dailyRevenue(dailyRevenue)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
