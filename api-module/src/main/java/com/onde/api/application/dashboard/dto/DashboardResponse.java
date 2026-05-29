package com.onde.api.application.dashboard.dto;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.settlement.AccountStatus;
import com.onde.core.entity.settlement.SellerAccount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {
    private String email;
    private String name;
    private String phoneNumber;
    private AccountStatus accountStatus;
    private String bankName;
    private String accountNumber;

    public static DashboardResponse of(Member member, SellerAccount sellerAccount) {
        return DashboardResponse.builder()
                .email(member.getEmail())
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .accountStatus(sellerAccount != null ? sellerAccount.getStatus() : null)
                .bankName(sellerAccount != null ? sellerAccount.getBankName() : null)
                .accountNumber(sellerAccount != null ? sellerAccount.getAccountNumber() : null)
                .build();
    }
}
