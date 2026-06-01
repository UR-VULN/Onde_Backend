package com.onde.api.application.insurance;

import com.onde.api.application.insurance.dto.SellerInsuranceRegisterRequest;
import com.onde.api.application.insurance.dto.SellerInsuranceRegisterResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerInsuranceController {

    private final SellerInsuranceService sellerInsuranceService;

    @PostMapping("/insurance")
    public ResponseEntity<ApiResponse<SellerInsuranceRegisterResponse>> proposeInsuranceProduct(
            @RequestBody SellerInsuranceRegisterRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        
        // 헤더 X-Member-Id 파싱 및 로컬 fallback 지원 (기본 판매자 ID = 2L)
        Long actualSellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                actualSellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // Ignore and fallback
            }
        }

        SellerInsuranceRegisterResponse response = sellerInsuranceService.proposeInsuranceProduct(req, actualSellerId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "신규 보험 상품 요율안이 등록 제안되었습니다. 본사 검수 후 노출 개시됩니다."));
    }

    @GetMapping("/insurances")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getInsurances() {
        java.util.List<com.onde.core.entity.insurance.InsuranceProduct> list = sellerInsuranceService.getAllProducts();
        java.util.List<java.util.Map<String, Object>> mapped = list.stream().map(ip -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", ip.getId());
            item.put("name", ip.getProductName());
            String status = "ACTIVE";
            if (ip.getStatus() == com.onde.core.entity.flight.ApprovalStatus.PENDING_APPROVAL) {
                status = "PENDING";
            } else if (ip.getStatus() == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", ip.getBaseDailyRate());
            return item;
        }).toList();

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("insurances", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 보험 목록 조회가 성공적으로 완료되었습니다."));
    }
}
