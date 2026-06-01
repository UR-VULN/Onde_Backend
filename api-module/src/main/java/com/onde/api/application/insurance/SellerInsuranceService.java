package com.onde.api.application.insurance;

import com.onde.api.application.insurance.dto.SellerInsuranceRegisterRequest;
import com.onde.api.application.insurance.dto.SellerInsuranceRegisterResponse;
import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.entity.insurance.InsuranceProduct;
import com.onde.core.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerInsuranceService {

    private final InsuranceProductRepository insuranceProductRepository;

    /**
     * [Day 5] 보험사 판매자의 신규 여행자 보험 요율 상품 등록 신청 제안
     */
    @Transactional
    public SellerInsuranceRegisterResponse proposeInsuranceProduct(SellerInsuranceRegisterRequest req, Long sellerId) {
        log.info("🛡️ Proposing new insurance product by sellerId={}, productName={}", sellerId, req.getProductName());

        // 1. 보험 상품 요율 및 한도 정보 엔티티 빌드 및 저장
        InsuranceProduct product = InsuranceProduct.builder()
                .productName(req.getProductName())
                .baseDailyRate(req.getBaseDailyRate())
                .coverageDetails(req.getCoverageDetails()) // JSON 데이터 바인딩
                .status(ApprovalStatus.PENDING_APPROVAL)
                .build();

        InsuranceProduct savedProduct = insuranceProductRepository.save(product);
        log.info("🎉 Proposed insurance product successfully. productId={}, status={}", savedProduct.getId(), savedProduct.getStatus());

        return SellerInsuranceRegisterResponse.builder()
                .productId(savedProduct.getId())
                .productName(savedProduct.getProductName())
                .status(savedProduct.getStatus())
                .build();
    }

    public java.util.List<InsuranceProduct> getAllProducts() {
        return insuranceProductRepository.findAll();
    }
}
