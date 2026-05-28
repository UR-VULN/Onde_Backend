package com.onde.api.application.settlement.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerAccountResponse {
    private Long sellerId;
    private String bankName;
    private String accountNumber; // 마스킹 처리된 계좌번호 반환
    private String accountHolder;
    private String businessNumber;
    private String representativeName;
    private String openedAt;
    private LocalDateTime createdAt;
}

