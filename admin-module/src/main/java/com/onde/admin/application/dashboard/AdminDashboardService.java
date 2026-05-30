package com.onde.admin.application.dashboard;

import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.entity.settlement.SettlementStatus;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.InsurancePolicyRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.SettlementRepository;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PostRepository;
import com.onde.core.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final ReservationRepository reservationRepository;
    private final FlightBookingRepository flightBookingRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PropertyRepository propertyRepository;

    /**
     * 일반 운영 지표 요약 실제 연동
     */
    public Map<String, Object> getOperationalMetrics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long newMembersToday = memberRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long activePostCount = postRepository.countByStatus(PostStatus.ACTIVE);
        long blindedPosts = postRepository.countByStatus(PostStatus.BLINDED);
        long unverifiedProperties = propertyRepository.countByIsVerified(false);
        long pendingCSTickets = 7; // 가상 CS 티켓 개수

        Map<String, Object> data = new HashMap<>();
        data.put("newMembersToday", newMembersToday);
        data.put("activePostCount", activePostCount);
        data.put("blindedPosts", blindedPosts);
        data.put("unverifiedProperties", unverifiedProperties);
        data.put("pendingCSTickets", pendingCSTickets);

        return data;
    }

    /**

     * 전사 총매출 대시보드 요약 실제 연동
     */
    public Map<String, Object> getSummary(String month) {
        // 1. month 파라미터 파싱 (yyyy-MM)
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        // 2. 도메인별 매출액 집계
        BigDecimal flightRevenue = flightBookingRepository.sumTotalPriceByStatusAndCreatedAtBetween(start, end);
        BigDecimal hotelRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.ROOM, start, end);
        BigDecimal carRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.CAR, start, end);
        BigDecimal insuranceRevenue = insurancePolicyRepository.sumTotalPremiumByStatusNotAndCreatedAtBetween(start, end);

        long totalRevenue = flightRevenue.add(hotelRevenue).add(carRevenue).add(insuranceRevenue).longValue();

        // 3. 예약 건수 집계
        long flightBookings = flightBookingRepository.countByStatusAndCreatedAtBetween(start, end);
        long accommodationBookings = reservationRepository.countByStatusNotAndCreatedAtBetween(start, end); // 숙소/렌터카 통합 카운트
        long insurancePolicies = insurancePolicyRepository.countByStatusNotAndCreatedAtBetween(start, end);

        long totalBookings = flightBookings + accommodationBookings + insurancePolicies;

        // 4. 대기 정산 건수 집계 (REQUESTED 상태)
        long pendingSettlements = settlementRepository.findByStatus(SettlementStatus.REQUESTED, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        // 5. 응답 구성
        Map<String, Object> byDomain = new HashMap<>();
        byDomain.put("flight", flightRevenue.longValue());
        byDomain.put("accommodation", hotelRevenue.longValue());
        byDomain.put("car", carRevenue.longValue());
        byDomain.put("insurance", insuranceRevenue.longValue());

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("totalBookings", totalBookings);
        data.put("pendingSettlements", pendingSettlements);
        data.put("byDomain", byDomain);

        return data;
    }

    /**
     * 도메인별 매출 비중 차트 실제 연동
     */
    public Map<String, Object> getCharts(String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        BigDecimal flightRevenue = flightBookingRepository.sumTotalPriceByStatusAndCreatedAtBetween(start, end);
        BigDecimal hotelRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.ROOM, start, end);
        BigDecimal carRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.CAR, start, end);
        BigDecimal insuranceRevenue = insurancePolicyRepository.sumTotalPremiumByStatusNotAndCreatedAtBetween(start, end);

        BigDecimal total = flightRevenue.add(hotelRevenue).add(carRevenue).add(insuranceRevenue);
        double totalVal = total.doubleValue();

        List<Map<String, Object>> segments = new ArrayList<>();

        segments.add(createSegment("항공", flightRevenue.longValue(), totalVal));
        segments.add(createSegment("숙소", hotelRevenue.longValue(), totalVal));
        segments.add(createSegment("렌터카", carRevenue.longValue(), totalVal));
        segments.add(createSegment("보험", insuranceRevenue.longValue(), totalVal));

        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "PIE");
        data.put("segments", segments);

        return data;
    }

    private Map<String, Object> createSegment(String domainName, long amount, double total) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("domain", domainName);
        segment.put("amount", amount);
        double ratio = total > 0 ? (double) amount / total : 0.0;
        segment.put("ratio", Math.round(ratio * 1000.0) / 1000.0); // 소수점 3자리 반올림
        return segment;
    }
}
