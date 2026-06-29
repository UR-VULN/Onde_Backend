package com.onde.api.application.insurance;

import com.onde.api.application.insurance.dto.InsuranceCalculateRequest;
import com.onde.api.application.insurance.dto.InsuranceCalculateResponse;
import com.onde.api.application.insurance.dto.InsurancePolicyRequest;
import com.onde.api.application.insurance.dto.InsurancePolicyResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @PostMapping({"/insurances/calculate", "/insurance/calculate"})
    public ResponseEntity<ApiResponse<InsuranceCalculateResponse>> calculatePremium(
            @RequestBody InsuranceCalculateRequest req) {
        
        InsuranceCalculateResponse response = insuranceService.calculatePremium(req);
        return ResponseEntity.ok(ApiResponse.success(response, "실시간 동적 보험 요율 사전 계산 결과를 조회했습니다."));
    }

    @PostMapping("/reservations/insurances")
    public ResponseEntity<ApiResponse<InsurancePolicyResponse>> applyPolicy(
            @RequestBody InsurancePolicyRequest req,
            @LoginMember Long actualUserId) {
        
        InsurancePolicyResponse response = insuranceService.applyPolicy(req, actualUserId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "여행자 보험 상품 최종 가입 및 체결이 성공적으로 완료되었습니다."));
    }
}
