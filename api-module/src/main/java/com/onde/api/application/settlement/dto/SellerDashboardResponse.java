package com.onde.api.application.settlement.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDashboardResponse {
    private String period;
    private Long totalRevenue;
    private List<RevenueBreakdown> breakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueBreakdown {
        private String month;
        private Long revenue;
        private Integer bookingCount;
    }
}

