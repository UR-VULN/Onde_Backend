package com.onde.api.application.membergrade;

import com.onde.core.entity.payment.PaymentStatus;
import com.onde.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 회원의 누적 결제액을 실시간으로 분석하여 회원 등급 및 혜택 정보를 계산하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberGradeService {

    private final PaymentRepository paymentRepository;

    /**
     * 특정 사용자의 누적 결제 완료 금액(status = PAID 상태인 결제건의 pgAmount 총합)을 조회 및 계산하고,
     * 그에 매칭되는 등급(BRONZE, SILVER, GOLD, VIP), 해당 등급의 마일리지 적립률, 다음 등급까지의 매출
     * 문턱값(threshold) 정보를 반환합니다.
     *
     * @param userId 회원 식별자
     * @return 등급명(grade), 적립율(rate), 다음 등급 기준금액(threshold)을 포함하는 맵 정보
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMemberGradeInfo(Long userId) {
        // 1. 전체 결제 데이터를 조회
        List<com.onde.core.entity.payment.Payment> payments = paymentRepository.findAll();

        // 2. 해당 회원이 실제로 결제에 성공하여 지불 완료한 금액(pgAmount)의 누적 총합을 실시간 집계
        long totalPgAmount = payments.stream()
                .filter(p -> p.getUserId() != null && p.getUserId().equals(userId)
                        && p.getStatus() == PaymentStatus.PAID)
                .mapToLong(p -> p.getPgAmount() != null ? p.getPgAmount() : 0)
                .sum();

        String grade;
        double accumulationRate;
        long nextThreshold;

        // 3. 누적 금액 기준에 따른 등급 및 혜택 분기 처리
        // - 500만원 이상: VIP (마일리지 적립률 1%, 최고 등급)
        // - 100만원 이상 ~ 500만원 미만: GOLD (마일리지 적립률 0.5%)
        // - 30만원 이상 ~ 100만원 미만: SILVER (마일리지 적립률 0.2%)
        // - 30만원 미만: BRONZE (마일리지 적립률 0.1%)
        if (totalPgAmount >= 5000000) {
            grade = "VIP";
            accumulationRate = 0.01;
            nextThreshold = 0; // 이미 최고 등급이므로 0
        } else if (totalPgAmount >= 1000000) {
            grade = "GOLD";
            accumulationRate = 0.005;
            nextThreshold = 5000000;
        } else if (totalPgAmount >= 300000) {
            grade = "SILVER";
            accumulationRate = 0.002;
            nextThreshold = 1000000;
        } else {
            grade = "BRONZE";
            accumulationRate = 0.001;
            nextThreshold = 300000;
        }

        Map<String, Object> gradeInfo = new HashMap<>();
        gradeInfo.put("grade", grade);
        gradeInfo.put("rate", accumulationRate);
        gradeInfo.put("threshold", nextThreshold);
        return gradeInfo;
    }
}
