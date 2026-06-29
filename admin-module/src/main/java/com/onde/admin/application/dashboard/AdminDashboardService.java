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

/**
 * [본사 관리자용 대시보드 비즈니스 로직 서비스]
 * 전사 매출 현황, 카테고리별 예약 건수, 정산 상태, 일일 운영 지표(회원 가입, 게시글 현황 등)를 
 * 종합적으로 집계 및 계산하여 관리자 페이지에 전달하기 위한 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    // 각 도메인 및 엔티티의 통계 조회를 위한 Repository 주입
    private final ReservationRepository reservationRepository;
    private final FlightBookingRepository flightBookingRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PropertyRepository propertyRepository;

    /**
     * [일일 운영 지표 및 서비스 요약 통계 조회]
     * 오늘 하루 신규 회원 수, 커뮤니티 활성/블라인드 게시글 개수, 
     * 미인증 등록 매물 좌표 개수 및 가상 CS 티켓 개수를 조회하여 맵 형태로 반환합니다.
     */
    public Map<String, Object> getOperationalMetrics() {
        // 오늘의 시작 시각(00:00:00)과 내일의 시작 시각(00:00:00) 설정
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // 1. 오늘 가입한 신규 회원 수 카운트
        long newMembersToday = memberRepository.countByCreatedAtBetween(todayStart, todayEnd);
        
        // 2. 현재 활성화되어 있는 전체 피드 게시글 개수 카운트
        long activePostCount = postRepository.countByStatus(PostStatus.ACTIVE);
        
        // 3. 관리자 또는 필터에 의해 블라인드 처리된 피드 게시글 개수 카운트
        long blindedPosts = postRepository.countByStatus(PostStatus.BLINDED);
        
        // 4. 좌표 변환은 완료되었으나 아직 본사 검수 검증(isVerified = false)이 완료되지 않은 파트너 매물 수 카운트
        long unverifiedProperties = propertyRepository.countByIsVerified(false);
        
        // 5. 미해결된 관리자 문의 대기 건수 (가상 CS 티켓 고정 개수 대조)
        long pendingCSTickets = 7; 

        // 조회한 일일 지표 데이터를 반환용 Map 객체에 매핑
        Map<String, Object> data = new HashMap<>();
        data.put("newMembersToday", newMembersToday);
        data.put("activePostCount", activePostCount);
        data.put("blindedPosts", blindedPosts);
        data.put("unverifiedProperties", unverifiedProperties);
        data.put("pendingCSTickets", pendingCSTickets);

        return data;
    }

    /**
     * [전사 월별 매출 및 예약 현황 종합 요약 정보 조회]
     * 지정한 월(yyyy-MM)을 기준으로 각 도메인(항공, 숙소, 렌터카, 보험)별 총 매출 합계, 
     * 총 예약 건수 및 대기 중인 파트너 정산 요청 건수를 집계합니다.
     */
    public Map<String, Object> getSummary(String month) {
        // 1. month 파라미터 파싱 (형식: "yyyy-MM" -> "2026-06")
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        // 대상 월의 시작 시각(1일 00:00:00)과 다음 달 시작 시각 생성
        LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        // 2. 각 도메인별 해당 월 매출액 데이터베이스 집계 (Repository Custom Query 활용)
        // - 항공권 매출액 집계
        BigDecimal flightRevenue = flightBookingRepository.sumTotalPriceByStatusAndCreatedAtBetween(start, end);
        // - 숙소 매출액 집계 (Reservation Target이 ROOM인 건)
        BigDecimal hotelRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.ROOM, start, end);
        // - 렌터카 매출액 집계 (Reservation Target이 CAR인 건)
        BigDecimal carRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.CAR, start, end);
        // - 여행자 보험 최종 납입 보험료 집계
        BigDecimal insuranceRevenue = insurancePolicyRepository.sumTotalPremiumByStatusNotAndCreatedAtBetween(start, end);

        // 네 가지 도메인의 매출 합계를 계산하고 Long 타입으로 치환
        long totalRevenue = flightRevenue.add(hotelRevenue).add(carRevenue).add(insuranceRevenue).longValue();

        // 3. 해당 월의 총 예약(가입) 건수 개별 집계
        // - 항공 예약 수량 카운트
        long flightBookings = flightBookingRepository.countByStatusAndCreatedAtBetween(start, end);
        // - 숙소 및 렌터카 예약 통합 수량 카운트
        long accommodationBookings = reservationRepository.countByStatusNotAndCreatedAtBetween(start, end); 
        // - 보험 최종 계약 체결 수량 카운트
        long insurancePolicies = insurancePolicyRepository.countByStatusNotAndCreatedAtBetween(start, end);

        // 전체 도메인 예약 수 합산
        long totalBookings = flightBookings + accommodationBookings + insurancePolicies;

        // 4. 현재 대기 중인 파트너 정산 요청 건수 집계 (REQUESTED 상태의 정산 건)
        long pendingSettlements = settlementRepository.findByStatus(SettlementStatus.REQUESTED, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        // 5. 응답 구성을 위한 데이터 매핑
        // 도메인별 매출액 저장
        Map<String, Object> byDomain = new HashMap<>();
        byDomain.put("flight", flightRevenue.longValue());
        byDomain.put("accommodation", hotelRevenue.longValue());
        byDomain.put("car", carRevenue.longValue());
        byDomain.put("insurance", insuranceRevenue.longValue());

        // 최종 통계 서머리 객체 생성
        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("totalBookings", totalBookings);
        data.put("pendingSettlements", pendingSettlements);
        data.put("byDomain", byDomain);

        return data;
    }

    /**
     * [도메인별 매출 비중 파이(PIE) 차트 데이터 조회]
     * 해당 월의 전사 매출액을 기준으로 각 도메인이 차지하는 비중(비율)을 계산하여 
     * 프론트엔드 차트 라이브러리 규격에 맞추어 전달합니다.
     */
    public Map<String, Object> getCharts(String month) {
        // month 파라미터 파싱 ("yyyy-MM")
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        // 대상 기간 설정
        LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        // 각 도메인별 월간 매출 계산
        BigDecimal flightRevenue = flightBookingRepository.sumTotalPriceByStatusAndCreatedAtBetween(start, end);
        BigDecimal hotelRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.ROOM, start, end);
        BigDecimal carRevenue = reservationRepository.sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(ReservationTarget.CAR, start, end);
        BigDecimal insuranceRevenue = insurancePolicyRepository.sumTotalPremiumByStatusNotAndCreatedAtBetween(start, end);

        // 전사 총매출액 계산
        BigDecimal total = flightRevenue.add(hotelRevenue).add(carRevenue).add(insuranceRevenue);
        double totalVal = total.doubleValue();

        // 차트용 세그먼트 리스트 구축
        List<Map<String, Object>> segments = new ArrayList<>();

        // 각 도메인별 점유율 세그먼트 생성 및 적재
        segments.add(createSegment("항공", flightRevenue.longValue(), totalVal));
        segments.add(createSegment("숙소", hotelRevenue.longValue(), totalVal));
        segments.add(createSegment("렌터카", carRevenue.longValue(), totalVal));
        segments.add(createSegment("보험", insuranceRevenue.longValue(), totalVal));

        // 최종 차트용 JSON 규격 구성
        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "PIE");
        data.put("segments", segments);

        return data;
    }

    /**
     * [차트 구성용 개별 세그먼트 데이터 포맷팅 헬퍼 메서드]
     * 도메인 이름, 해당 도메인의 매출액 및 총매출 대비 비율(소수점 3자리 반올림)을 계산하여 반환합니다.
     */
    private Map<String, Object> createSegment(String domainName, long amount, double total) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("domain", domainName);
        segment.put("amount", amount);
        
        // 비율 계산 (총합이 0보다 클 때만 나눗셈 수행)
        double ratio = total > 0 ? (double) amount / total : 0.0;
        
        // 소수점 3자리 반올림 처리 (예: 0.1234 -> 0.123)
        segment.put("ratio", Math.round(ratio * 1000.0) / 1000.0); 
        
        return segment;
    }
}
