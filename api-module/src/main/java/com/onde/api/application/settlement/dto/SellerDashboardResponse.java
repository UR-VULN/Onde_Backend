package com.onde.api.application.settlement.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDashboardResponse {
    private String period;
    private BigDecimal totalRevenue;
    private List<RevenueBreakdown> breakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueBreakdown {
        private String month;
        private BigDecimal revenue;
        private Integer bookingCount;
    }
}

