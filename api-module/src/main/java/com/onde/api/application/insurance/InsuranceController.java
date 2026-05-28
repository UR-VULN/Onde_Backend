package com.onde.api.application.insurance;

import com.onde.api.application.insurance.dto.InsuranceCalculateRequest;
import com.onde.api.application.insurance.dto.InsuranceCalculateResponse;
import com.onde.api.application.insurance.dto.InsurancePolicyRequest;
import com.onde.api.application.insurance.dto.InsurancePolicyResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<InsuranceCalculateResponse>> calculatePremium(
            @RequestBody InsuranceCalculateRequest req) {
        
        InsuranceCalculateResponse response = insuranceService.calculatePremium(req);
        return ResponseEntity.ok(ApiResponse.success(response, "실시간 동적 보험 요율 사전 계산 결과를 조회했습니다."));
    }

    @PostMapping("/policies")
    public ResponseEntity<ApiResponse<InsurancePolicyResponse>> applyPolicy(
            @RequestBody InsurancePolicyRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        
        // 헤더 X-Member-Id 파싱 및 로컬 fallback 지원
        Long actualUserId = 1L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                actualUserId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // Ignore and fallback
            }
        }

        InsurancePolicyResponse response = insuranceService.applyPolicy(req, actualUserId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "여행자 보험 상품 최종 가입 및 체결이 성공적으로 완료되었습니다."));
    }
}
