package com.onde.api.application.settlement;

import com.onde.core.support.ApiResponse;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 정산 계좌 및 사업자 정보 관리를 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/seller/account")
@RequiredArgsConstructor
public class SellerAccountController {

    private final NtsBusinessVerificationService ntsBusinessVerificationService;

    /**
     * 판매자 사업자등록번호의 진위 여부를 국세청 API를 통해 검증합니다.
     *
     * @param request 사업자등록번호, 대표자명, 개업일자를 담은 DTO
     * @return 검증 성공 여부 결과 (true/false)
     */
    @PostMapping("/verify-business")
    public ResponseEntity<ApiResponse<VerifyBusinessResponse>> verifyBusiness(@RequestBody VerifyBusinessRequest request) {
        NtsBusinessVerificationService.BusinessVerificationResult result =
                ntsBusinessVerificationService.verifyBusiness(
                        request.getBusinessNumber(),
                        request.getRepresentativeName(),
                        request.getOpenDate()
                );
        VerifyBusinessResponse response = VerifyBusinessResponse.builder()
                .verified(result.verified())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, result.message()));
    }

    /**
     * 사업자 진위 확인 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessRequest {
        private String businessNumber;      // 사업자등록번호
        private String representativeName;   // 대표자성명
        private String openDate;             // 개업일자 (YYYYMMDD)
    }

    /**
     * 사업자 진위 확인 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessResponse {
        private boolean verified;            // 검증 완료 여부
    }
}
