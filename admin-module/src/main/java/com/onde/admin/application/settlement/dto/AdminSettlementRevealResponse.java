package com.onde.admin.application.settlement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSettlementRevealResponse {
    private Long settlementId;
    private String sellerName;
    private String bankName;
    private String accountNumber;
}
