package com.onde.api.application.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

/** 판매자 대시보드 민감 필드 원문 (기본 dashboard API는 마스킹) */
@Getter
@Builder
public class SellerDashboardRevealResponse {
    private String email;
    private String bankName;
    private String accountNumber;
}
