package com.onde.api.application.payment;

import com.onde.api.application.payment.dto.request.WalletChargeRequest;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/members/me/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getBalance(@LoginMember Long userId) {
        BigDecimal balance = walletService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(new WalletBalanceResponse(balance), "지갑 잔액 조회 성공"));
    }

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> chargeWallet(
            @LoginMember Long userId,
            @Valid @RequestBody WalletChargeRequest request) {
        BigDecimal newBalance = walletService.charge(userId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(new WalletBalanceResponse(newBalance), "지갑 충전 성공"));
    }

    @Getter
    public static class WalletBalanceResponse {
        private final BigDecimal balance;

        public WalletBalanceResponse(BigDecimal balance) {
            this.balance = balance;
        }
    }
}
