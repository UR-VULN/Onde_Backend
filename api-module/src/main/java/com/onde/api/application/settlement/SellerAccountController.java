package com.onde.api.application.settlement;

import com.onde.core.support.ApiResponse;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/account")
@RequiredArgsConstructor
public class SellerAccountController {

    private final NtsBusinessVerificationService ntsBusinessVerificationService;

    @PostMapping("/verify-business")
    public ResponseEntity<ApiResponse<VerifyBusinessResponse>> verifyBusiness(@RequestBody VerifyBusinessRequest request) {
        boolean verified = ntsBusinessVerificationService.verifyBusinessNumber(request.getBusinessNumber());
        VerifyBusinessResponse response = VerifyBusinessResponse.builder()
                .verified(verified)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "사업자 진위 확인에 성공하였습니다."));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessRequest {
        private String businessNumber;
        private String representativeName;
        private String openDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessResponse {
        private boolean verified;
    }
}
