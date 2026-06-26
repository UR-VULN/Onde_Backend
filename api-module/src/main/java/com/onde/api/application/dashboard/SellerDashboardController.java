package com.onde.api.application.dashboard;

import com.onde.api.application.dashboard.dto.DashboardResponse;
import com.onde.api.application.dashboard.dto.SellerDashboardRevealResponse;
import com.onde.api.application.member.SensitiveRevealAuthService;
import com.onde.api.application.member.dto.SensitiveRevealPasswordRequest;
import com.onde.api.security.CustomUserDetails;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;
    private final SensitiveRevealAuthService sensitiveRevealAuthService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DashboardResponse response = sellerDashboardService.getDashboardInfo(userDetails.getMember());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reveal")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<SellerDashboardRevealResponse>> revealDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SensitiveRevealPasswordRequest request) {

        sensitiveRevealAuthService.requirePasswordVerifiedMember(
                userDetails.getMember().getId(),
                request.getPassword());
        SellerDashboardRevealResponse response = sellerDashboardService.getDashboardReveal(userDetails.getMember());
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 민감 정보 원문 조회 성공"));
    }
}
