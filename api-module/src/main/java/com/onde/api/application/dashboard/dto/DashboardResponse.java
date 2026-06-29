package com.onde.api.application.dashboard.dto;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.settlement.AccountStatus;
import com.onde.core.entity.settlement.SellerAccount;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class DashboardResponse {
    private String email;
    private AccountStatus accountStatus;
    private String bankName;
    private String accountNumber;
    
    // 신규 추가: 판매 상품 수 통계
    private long accommodationCount;
    private long carCount;
    private long flightRouteCount;
    
    // 신규 추가: 항목별 매출 통계
    private long stayRevenue;
    private long carRevenue;
    private long flightRevenue;
    private long totalRevenue;
    
    // 신규 추가: 최근 통합 예약 현황
    private List<RecentReservationDto> recentReservations;

    @Getter
    @Builder
    public static class RecentReservationDto {
        private Long id;
        private String customerName;
        private String targetType; // "STAY", "CAR", "FLIGHT"
        private String productName;
        private String schedule;
        private Long price;
        private String status;
    }

    public static DashboardResponse of(
            Member member, 
            SellerAccount sellerAccount,
            long accommodationCount,
            long carCount,
            long flightRouteCount,
            long stayRevenue,
            long carRevenue,
            long flightRevenue,
            long totalRevenue,
            List<RecentReservationDto> recentReservations) {
            
        return DashboardResponse.builder()
                .email(member.getEmail())
                .accountStatus(sellerAccount != null ? sellerAccount.getStatus() : null)
                .bankName(sellerAccount != null ? sellerAccount.getBankName() : null)
                .accountNumber(sellerAccount != null ? sellerAccount.getAccountNumber() : null)
                .accommodationCount(accommodationCount)
                .carCount(carCount)
                .flightRouteCount(flightRouteCount)
                .stayRevenue(stayRevenue)
                .carRevenue(carRevenue)
                .flightRevenue(flightRevenue)
                .totalRevenue(totalRevenue)
                .recentReservations(recentReservations)
                .build();
    }
}

