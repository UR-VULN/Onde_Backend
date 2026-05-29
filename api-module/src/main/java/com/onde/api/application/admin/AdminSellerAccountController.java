package com.onde.api.application.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class AdminSellerAccountController {
    private final AdminSellerAccountService adminSellerAccountService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        adminSellerAccountService.approveAccount(id);
        return ResponseEntity.ok("정산 계좌 승인 완료");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> reject(@PathVariable Long id) {
        adminSellerAccountService.rejectAccount(id);
        return ResponseEntity.ok("정산 계좌 반려 완료");
    }
}
