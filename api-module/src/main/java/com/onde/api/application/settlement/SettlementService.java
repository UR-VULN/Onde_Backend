package com.onde.api.application.settlement;

import com.onde.core.entity.payment.PaymentStatus;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SellerAccountRepository;
import com.onde.core.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.onde.api.application.payment.dto.response.SellerStatisticsResponse;
import com.onde.api.application.payment.dto.response.PlatformStatisticsResponse;

/**
 * 정산 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 일별 정산 기초 데이터 생성(배치용), 판매자의 지급 신청,
 * 그리고 본사 관리자의 1차 승인 및 최종 확정(COMPLETED) 프로세스를 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SellerAccountRepository sellerAccountRepository;

    /**
     * 특정 일자의 전체 결제 완료(PAID) 데이터를 기반으로 판매자별 정산 대기(PENDING) 레코드를 생성합니다.
     * 플랫폼 중개 수수료는 매출액의 3%로 책정되며, 일별 1차 정산 기초 데이터를 만들기 위해 스케줄러(배치) 등에 의해 동작합니다.
     *
     * @param targetDate 정산을 진행할 대상 일자 (예: 2026-05-27)
     */
    @Transactional
    public void executeDailySettlement(LocalDate targetDate) {
        // 1. 대상 일자의 시작 일시(00:00:00)와 종료 일시(다음날 00:00:00 미만) 계산
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        // 2. 해당 기간 동안의 PAID 상태 결제 내역을 판매자별로 그룹핑하여 집계 조회
        List<PaymentRepository.SettlementProjection> projections = paymentRepository.calculateSettlementAmounts(
                PaymentStatus.PAID, start, end);

        // 3. 집계된 판매자별 매출을 순회하며 정산 데이터(Settlement) 생성
        for (PaymentRepository.SettlementProjection proj : projections) {
            Long sellerId = proj.getSellerId();
            Long grossAmount = proj.getGrossAmount();

            if (sellerId == null || grossAmount == null || grossAmount == 0)
                continue;

            // 4. 플랫폼 중개 수수료 3% 계산 및 최종 지급액(netAmount = grossAmount - 수수료) 산정
            Long commission = (long) (grossAmount * 0.03);
            Long netAmount = grossAmount - commission;

            // 5. 정산 대기(PENDING) 상태로 엔티티 빌드 및 저장
            Settlement settlement = Settlement.builder()
                    .sellerId(sellerId)
                    .settlementDate(targetDate)
                    .grossAmount(grossAmount)
                    .commission(commission)
                    .netAmount(netAmount)
                    .status(SettlementStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            settlementRepository.save(settlement);
        }
    }

    /**
     * 판매자가 정산 대기(PENDING) 상태인 정산 건에 대해 정산 지급을 신청합니다.
     * 정산 계좌(SellerAccount)가 정상 등록되어 있는지 필수로 사전 검증합니다.
     *
     * @param settlementId 정산 식별자 (PK)
     * @param sellerId     지급을 요청하는 판매자 식별자 (본인 검증용)
     * @return 상태가 REQUESTED로 업데이트된 정산 정보
     */
    @Transactional
    public Settlement requestSettlement(Long settlementId, Long sellerId) {
        // 1. 대상 정산 건 조회
        Settlement settlement = settlementRepository.findById(settlementId)
                 .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));
 
        // 2. 권한 검증: 본인 점포의 정산 건인지 확인
        if (!settlement.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("본인의 정산 건만 신청 가능합니다.");
        }
 
        // 3. 상태 검증: 오직 PENDING 상태에서만 신청 가능
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("정산 대기(PENDING) 상태일 때만 신청 가능합니다.");
        }
 
        // 4. 계좌 정보 검증: 정산 지급을 받기 위해 계좌 정보(사업자 번호 포함)가 반드시 디비에 존재해야 함
        SellerAccount account = sellerAccountRepository.findBySellerId(sellerId)
                 .orElseThrow(() -> new IllegalArgumentException("정산 계좌가 등록되지 않았습니다."));

        if (account.getBusinessNumber() == null || account.getBusinessNumber().isEmpty()
                || account.getRepresentativeName() == null || account.getRepresentativeName().isEmpty()
                || account.getOpenedAt() == null || account.getOpenedAt().isEmpty()) {
            throw new IllegalArgumentException("정산 계좌의 사업자 정보(사업자등록번호, 대표자명, 개업일자)가 미등록 상태입니다.");
        }
 
        // 5. 정산 요청 상태(REQUESTED) 및 요청시각 설정
        settlement.setStatus(SettlementStatus.REQUESTED);
        settlement.setRequestedAt(LocalDateTime.now());
        return settlement;
    }
 
    /**
     * 본사의 1차 정산 담당자(SALES_ADMIN)가 판매자의 지급 신청(REQUESTED) 건을 검토한 후 승인합니다.
     * 상태가 APPROVED_1ST로 전이됩니다.
     *
     * @param settlementId 1차 승인할 정산 건의 식별자
     * @param comment      정산 담당자의 검토 코멘트 및 메모
     * @return 1차 승인이 완료된 정산 정보
     */
    @Transactional
    public Settlement approveFirstSettlement(Long settlementId, String comment) {
        // 1. 대상 정산 건 조회
        Settlement settlement = settlementRepository.findById(settlementId)
                 .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));
 
        // 2. 상태 검증: 오직 REQUESTED 상태에서만 1차 승인 가능
        if (settlement.getStatus() != SettlementStatus.REQUESTED) {
            throw new IllegalStateException("정산 요청(REQUESTED) 상태에서만 1차 승인이 가능합니다.");
        }
 
        // 3. 1차 승인 상태(APPROVED_1ST)로 변경 및 일시 기록
        settlement.setStatus(SettlementStatus.APPROVED_1ST);
        settlement.setApprovedAt(LocalDateTime.now());
        return settlement;
    }
 
    /**
     * 본사 최고 관리자(SUPER_ADMIN)가 1차 검토 완료(APPROVED_1ST)된 정산 건을 최종 승인 및 지급 완료 처리합니다.
     * 상태가 COMPLETED로 전이되며 정산 프로세스가 종결됩니다.
     *
     * @param settlementId 최종 지급 확정할 정산 건의 식별자
     * @param comment      최종 관리자의 승인 및 이체 메모
     * @return 최종 확정 완료된 정산 정보
     */
    @Transactional
    public Settlement finalizeSettlement(Long settlementId, String comment) {
        // 1. 대상 정산 건 조회
        Settlement settlement = settlementRepository.findById(settlementId)
                 .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));
 
        // 2. 상태 검증: 오직 APPROVED_1ST 상태에서만 최종 지급 확정 가능
        if (settlement.getStatus() != SettlementStatus.APPROVED_1ST) {
            throw new IllegalStateException("1차 승인(APPROVED_1ST) 상태에서만 최종 확정이 가능합니다.");
        }
 
        // 3. 정산 완료 상태(COMPLETED)로 변경 및 일시 기록
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setFinalizedAt(LocalDateTime.now());
        return settlement;
    }

    /**
     * 테스트용 정산 계좌 등록/수정 비즈니스 로직입니다.
     */
    @Transactional
    public SellerAccount registerOrUpdateAccount(Long sellerId, com.onde.api.application.settlement.dto.SellerAccountRequest req) {
        if (req.getBankName() == null || req.getAccountNumber() == null || req.getAccountHolder() == null) {
            throw new IllegalArgumentException("필수값 누락");
        }
        // 간단한 국세청 검증 Mocking
        if (req.getBusinessNumber() == null || req.getBusinessNumber().isEmpty()) {
            throw new IllegalArgumentException("국세청 검증 실패 - 유효하지 않은 사업자 번호");
        }

        SellerAccount account = sellerAccountRepository.findBySellerId(sellerId)
                .orElse(SellerAccount.builder().sellerId(sellerId).build());

        account.setBankName(req.getBankName());
        account.setAccountNumber(req.getAccountNumber()); // 실제 암호화 처리는 다른 작업자 몫이므로 테스트용으로 그대로 담음
        account.setAccountHolder(req.getAccountHolder());
        account.setBusinessNumber(req.getBusinessNumber());
        account.setRepresentativeName(req.getRepresentativeName());
        account.setOpenedAt(req.getOpenedAt());

        return sellerAccountRepository.save(account);
    }

    /**
     * 테스트용 정산 계좌 조회 비즈니스 로직입니다.
     */
    @Transactional(readOnly = true)
    public SellerAccount getAccount(Long sellerId) {
        return sellerAccountRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 계좌 없음"));
    }

    /**
     * 계좌번호 마스킹 유틸리티 (예: 123-456-789012 -> 123-***-***012)
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        // 하이픈이 있는 포맷인 경우: 123-456-789012 -> 123-***-***012
        if (accountNumber.contains("-")) {
            String[] parts = accountNumber.split("-");
            if (parts.length >= 3) {
                StringBuilder sb = new StringBuilder();
                sb.append(parts[0]).append("-");
                for (int i = 1; i < parts.length - 1; i++) {
                    sb.append("*".repeat(parts[i].length())).append("-");
                }
                sb.append(parts[parts.length - 1]);
                return sb.toString();
            }
        }
        // 일반 숫자인 경우 가운데 영역 마스킹
        int len = accountNumber.length();
        return accountNumber.substring(0, 3) + "***" + accountNumber.substring(len - 3);
    }


    /**
     * 특정 판매자의 일별/월별 매출 및 누적 매출 추이 통계를 조회합니다.
     *
     * @param sellerId 조회 대상 판매자 식별자
     * @return 일별/월별 누적 통계 트렌드가 포함된 DTO
     */
    @Transactional(readOnly = true)
    public SellerStatisticsResponse getSellerStatistics(Long sellerId) {
        // 1. 일별 정산 매출 데이터를 집계 조회
        List<SettlementRepository.RevenueProjection> dailyProjections = settlementRepository.findDailyRevenueBySellerId(sellerId);
        List<SellerStatisticsResponse.RevenueTrend> dailyTrends = new ArrayList<>();
        
        long dailyAccumulatedGross = 0;
        long dailyAccumulatedNet = 0;
        
        // 2. 일별 데이터를 순회하며 누적 매출액 계산
        for (SettlementRepository.RevenueProjection proj : dailyProjections) {
            dailyAccumulatedGross += proj.getGrossAmount();
            dailyAccumulatedNet += proj.getNetAmount();
            
            dailyTrends.add(SellerStatisticsResponse.RevenueTrend.builder()
                    .label(proj.getLabel())
                    .grossAmount(proj.getGrossAmount())
                    .netAmount(proj.getNetAmount())
                    .accumulatedGrossAmount(dailyAccumulatedGross)
                    .accumulatedNetAmount(dailyAccumulatedNet)
                    .build());
        }

        // 3. 월별 정산 매출 데이터를 집계 조회
        List<SettlementRepository.RevenueProjection> monthlyProjections = settlementRepository.findMonthlyRevenueBySellerId(sellerId);
        List<SellerStatisticsResponse.RevenueTrend> monthlyTrends = new ArrayList<>();
        
        long monthlyAccumulatedGross = 0;
        long monthlyAccumulatedNet = 0;
        
        // 4. 월별 데이터를 순회하며 누적 매출액 계산
        for (SettlementRepository.RevenueProjection proj : monthlyProjections) {
            monthlyAccumulatedGross += proj.getGrossAmount();
            monthlyAccumulatedNet += proj.getNetAmount();
            
            monthlyTrends.add(SellerStatisticsResponse.RevenueTrend.builder()
                    .label(proj.getLabel())
                    .grossAmount(proj.getGrossAmount())
                    .netAmount(proj.getNetAmount())
                    .accumulatedGrossAmount(monthlyAccumulatedGross)
                    .accumulatedNetAmount(monthlyAccumulatedNet)
                    .build());
        }

        return SellerStatisticsResponse.builder()
                .sellerId(sellerId)
                .dailyTrends(dailyTrends)
                .monthlyTrends(monthlyTrends)
                .build();
    }

    /**
     * 플랫폼 전사 총거래액(GMV), 수수료 순이익 및 서비스별 매출 비중 통계를 조회합니다.
     *
     * @return 전사 매출 성과 및 서비스 점유율 데이터가 포함된 DTO
     */
    @Transactional(readOnly = true)
    public PlatformStatisticsResponse getPlatformStatistics() {
        // 1. 플랫폼 전사 총거래액 및 수수료 누적 수익 조회
        PaymentRepository.PlatformRevenueProjection revenueProj = paymentRepository.calculatePlatformRevenue(PaymentStatus.PAID);
        
        long totalGmv = revenueProj.getTotalGmv() != null ? revenueProj.getTotalGmv() : 0L;
        long totalCommission = revenueProj.getTotalCommission() != null ? revenueProj.getTotalCommission() : 0L;

        // 2. 서비스 카테고리별(ROOM, CAR 등) 매출액 집계 조회
        List<PaymentRepository.ServiceRevenueProjection> serviceProjections = paymentRepository.calculateRevenueByService(PaymentStatus.PAID);
        List<PlatformStatisticsResponse.ServiceRevenueShare> serviceShares = new ArrayList<>();

        // 3. 서비스별 매출 점유 비중 계산
        for (PaymentRepository.ServiceRevenueProjection proj : serviceProjections) {
            long serviceGmv = proj.getServiceGmv() != null ? proj.getServiceGmv() : 0L;
            double shareRate = totalGmv > 0 ? (double) serviceGmv / totalGmv : 0.0;

            serviceShares.add(PlatformStatisticsResponse.ServiceRevenueShare.builder()
                    .serviceType(proj.getServiceType())
                    .grossAmount(serviceGmv)
                    .shareRate(shareRate)
                    .build());
        }

        return PlatformStatisticsResponse.builder()
                .totalGmv(totalGmv)
                .totalCommission(totalCommission)
                .serviceShares(serviceShares)
                .build();
    }
}
