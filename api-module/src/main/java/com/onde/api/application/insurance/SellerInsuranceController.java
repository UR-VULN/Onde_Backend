package com.onde.api.application.insurance;

import com.onde.api.application.insurance.dto.SellerInsuranceRegisterRequest;
import com.onde.api.application.insurance.dto.SellerInsuranceRegisterResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/insurance")
@RequiredArgsConstructor
public class SellerInsuranceController {

    private final SellerInsuranceService sellerInsuranceService;

    @PostMapping
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
}
