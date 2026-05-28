package com.onde.api.application.settlement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerAccountResponse {
    private String bankName; // 은행명
    private String accountNumber; // 계좌번호
    private String accountHolder; // 예금주명
    private String businessNumber; // 사업자등록번호
    private String representativeName; // 대표자명
    private String openedAt; // 개업일자
}