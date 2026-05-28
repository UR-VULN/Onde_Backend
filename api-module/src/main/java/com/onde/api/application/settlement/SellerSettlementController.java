package com.onde.api.application.settlement;

import com.onde.api.application.settlement.dto.SellerAccountRequest;
import com.onde.api.application.settlement.dto.SellerAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/settlements")
@RequiredArgsConstructor
public class SellerSettlementController {

    private final SellerSettlementService sellerSettlementService;

    // 판매자 정산 계좌 및 사업자 정보 등록/수정 
    @PutMapping("/accounts")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> registerAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SellerAccountRequest request) {
        
        //SecurityContextHolder에서 추출한 유저 정보(이메일)로 본인 검증 및 저장
        sellerSettlementService.registerAccount(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    // 판매자 정산 계좌 및 사업자 정보 조회
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerAccountResponse> getAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
         
        SellerAccountResponse response = sellerSettlementService.getAccount(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}