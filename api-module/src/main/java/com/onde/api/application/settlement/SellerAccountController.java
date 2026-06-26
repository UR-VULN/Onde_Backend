package com.onde.api.application.settlement;

import com.onde.core.support.ApiResponse;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    public ResponseEntity<ApiResponse<VerifyBusinessResponse>> verifyBusiness(
            @Valid @RequestBody VerifyBusinessRequest request) {
        NtsBusinessVerificationService.BusinessVerificationResult result =
                ntsBusinessVerificationService.verifyBusiness(
                        request.getBusinessNumber(),
                        request.getRepresentativeName(),
                        request.getOpenDate()
                );
        VerifyBusinessResponse response = VerifyBusinessResponse.builder()
                .verified(result.verified())
                .businessStatusCode(result.businessStatusCode())
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

        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 10자리 숫자여야 합니다.")
        private String businessNumber;

        @NotBlank(message = "대표자명은 필수입니다.")
        @Size(max = ValidationLimits.NAME_MAX, message = "대표자명은 100자 이하여야 합니다.")
        private String representativeName;

        @NotBlank(message = "개업일자는 필수입니다.")
        @Pattern(regexp = "^\\d{8}$", message = "개업일자는 YYYYMMDD 형식이어야 합니다.")
        private String openDate;
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
        private String businessStatusCode;   // 계속사업자(01), 휴업(02), 폐업(03)
    }
}
