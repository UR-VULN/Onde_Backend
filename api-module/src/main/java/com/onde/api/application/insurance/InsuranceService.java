package com.onde.api.application.insurance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onde.api.application.insurance.dto.InsuranceCalculateRequest;
import com.onde.api.application.insurance.dto.InsuranceCalculateResponse;
import com.onde.api.application.insurance.dto.InsurancePolicyRequest;
import com.onde.api.application.insurance.dto.InsurancePolicyResponse;
import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.entity.insurance.InsurancePolicy;
import com.onde.core.entity.insurance.InsurancePolicyStatus;
import com.onde.core.entity.insurance.InsuranceProduct;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.InsurancePolicyRepository;
import com.onde.core.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceService {

    private final InsuranceProductRepository insuranceProductRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final ObjectMapper objectMapper;

    /**
     * [Day 4] 실시간 동적 보험료 사전 계산 (만 나이, 여행 기간, 보장등급별 가산 요율 알고리즘 엔진)
     */
    public InsuranceCalculateResponse calculatePremium(InsuranceCalculateRequest req) {
        log.info("🛡️ Calculating dynamic insurance premium for product={}, coverageLevel={}", req.getInsuranceProductId(), req.getCoverageLevel());

        // 1. 보험 상품 조회
        InsuranceProduct product = insuranceProductRepository.findById(req.getInsuranceProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
        if (product.getStatus() != ApprovalStatus.APPROVED) {
            throw new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
        }

        // 2. 여행 기간(일 수) 연산
        int tripDurationDays = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
        if (tripDurationDays <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 3. 여행 시작일 기준 피보험자의 만 나이 연산
        int age = Period.between(req.getBirthdate(), req.getStartDate()).getYears();
        if (age < 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 4. 연령별 가산 요율 매핑 (Age Multiplier)
        BigDecimal ageMultiplier;
        if (age < 20) {
            ageMultiplier = BigDecimal.valueOf(0.8);
        } else if (age < 40) {
            ageMultiplier = BigDecimal.valueOf(1.0); // 표준 청년
        } else if (age < 60) {
            ageMultiplier = BigDecimal.valueOf(1.25); // 장년
        } else {
            ageMultiplier = BigDecimal.valueOf(1.6); // 고령/고위험군
        }

        // 5. 보장 등급별 가산 요율 매핑 (Coverage Multiplier)
        BigDecimal coverageMultiplier;
        String level = req.getCoverageLevel().trim().toUpperCase();
        switch (level) {
            case "STANDARD":
                coverageMultiplier = BigDecimal.valueOf(1.0);
                break;
            case "DELUXE":
                coverageMultiplier = BigDecimal.valueOf(1.5);
                break;
            case "PREMIUM":
                coverageMultiplier = BigDecimal.valueOf(2.2);
                break;
            default:
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 6. 기본 하루 요율 및 할증 미적용 기본 보험료 연산
        BigDecimal baseDailyRate = product.getBaseDailyRate();
        BigDecimal basePremiumWithoutMultipliers = baseDailyRate.multiply(BigDecimal.valueOf(tripDurationDays));

        // 7. 동적 할증 배율 결합 최종 산출
        BigDecimal calculated = basePremiumWithoutMultipliers.multiply(ageMultiplier).multiply(coverageMultiplier);

        // 8. 금융 데이터 신뢰성을 위한 원화 10원 단위 미만 즉시 절사 (Floor) 처리
        BigDecimal finalPremium = calculated.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);

        log.info("🛡️ Premium calculation completed. Calculated={}, FinalPremium={}", calculated, finalPremium);

        return InsuranceCalculateResponse.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .travelDays(tripDurationDays)
                .baseDailyRate(baseDailyRate)
                .totalPremium(finalPremium)
                .coverageDetails(parseCoverageDetails(product.getCoverageDetails()))
                .build();
    }

    /**
     * [Day 4] 여행자 보험 최종 가입 신청 (백엔드 2중 요금 위조 검증 및 영속화)
     */
    @Transactional
    public InsurancePolicyResponse applyPolicy(InsurancePolicyRequest req, Long userId) {
        log.info("🛡️ Finalizing insurance policy contract for user={}, premium={}", userId, req.getTotalPremium());

        // 1. 백엔드 자체 동적 보험료 재산출 (금액 위조 방어 목적)
        InsuranceCalculateRequest calcReq = InsuranceCalculateRequest.builder()
                .insuranceProductId(req.getInsuranceProductId())
                .birthdate(req.getInsuredBirthdate())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .coverageLevel(req.getCoverageLevel())
                .build();

        InsuranceCalculateResponse calculated = calculatePremium(calcReq);

        // 2. 2중 요금 대조 검증 (Cross-Verification) 집행
        BigDecimal requestedPremium = req.getTotalPremium() != null ? req.getTotalPremium() : calculated.getTotalPremium();
        if (calculated.getTotalPremium().compareTo(requestedPremium) != 0) {
            log.error("❌ Insurance premium 위조 또는 일치하지 않는 가입 시도 감지. Expected={}, Inputted={}",
                    calculated.getTotalPremium(), requestedPremium);
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 3. 보험 상품 조회
        InsuranceProduct product = insuranceProductRepository.findById(req.getInsuranceProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
        if (product.getStatus() != ApprovalStatus.APPROVED) {
            throw new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
        }

        // 4. 고유 계약 가입 코드 생성
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String policyCode = "POL-" + todayStr + "-" + randomSuffix;

        // 5. 보험 가입(계약) 정보 영속화
        InsurancePolicy policy = InsurancePolicy.builder()
                .policyCode(policyCode)
                .insuranceProduct(product)
                .userId(userId)
                .insuredName(req.getInsuredName())
                .insuredBirthdate(req.getInsuredBirthdate())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .coverageLevel(req.getCoverageLevel().trim().toUpperCase())
                .totalPremium(calculated.getTotalPremium())
                .status(InsurancePolicyStatus.ACTIVE)
                .build();

        InsurancePolicy savedPolicy = insurancePolicyRepository.save(policy);
        log.info("🎉 Policy registration successfully. policyCode={}, premium={}", savedPolicy.getPolicyCode(), savedPolicy.getTotalPremium());

        return InsurancePolicyResponse.builder()
                .policyId(savedPolicy.getId())
                .policyCode(savedPolicy.getPolicyCode())
                .productName(product.getProductName())
                .insuredName(savedPolicy.getInsuredName())
                .startDate(savedPolicy.getStartDate())
                .endDate(savedPolicy.getEndDate())
                .coverageLevel(savedPolicy.getCoverageLevel())
                .totalPremium(savedPolicy.getTotalPremium())
                .status(savedPolicy.getStatus().name())
                .build();
    }

    private Object parseCoverageDetails(String coverageDetails) {
        try {
            return objectMapper.readValue(coverageDetails, new TypeReference<java.util.Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Coverage details are not valid JSON object. Returning raw value.");
            return coverageDetails;
        }
    }
}
