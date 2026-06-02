package com.onde.admin.application.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/seller-accounts")
@RequiredArgsConstructor
public class AdminSellerAccountController {
    private final AdminSellerAccountService adminSellerAccountService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        adminSellerAccountService.approveAccount(id);
        return ResponseEntity.ok("정산 계좌 승인 완료");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> reject(@PathVariable Long id) {
        adminSellerAccountService.rejectAccount(id);
        return ResponseEntity.ok("정산 계좌 반려 완료");
    }
}
