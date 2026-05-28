package com.onde.api.application.settlement.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerAccountRequest {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String businessNumber;
    private String representativeName;
    private String openedAt;
}
