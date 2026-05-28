package com.onde.api.application.admin.dto;

import com.onde.core.entity.settlement.SellerAccount;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementResponse {
    private Long id;
    private String sellerEmail;
    private String bankName;
    private String accountNumber;
    private String status;
    private LocalDateTime createdAt;

    public static SettlementResponse from(SellerAccount account) {
        return SettlementResponse.builder()
                .id(account.getId())
                .sellerEmail(account.getMember().getEmail())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }
}