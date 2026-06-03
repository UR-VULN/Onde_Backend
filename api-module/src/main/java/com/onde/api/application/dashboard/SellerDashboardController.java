package com.onde.api.application.dashboard;

import com.onde.api.application.dashboard.dto.DashboardResponse;
import com.onde.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        DashboardResponse response = sellerDashboardService.getDashboardInfo(userDetails.getMember());
        
        return ResponseEntity.ok(response);
    }
}
