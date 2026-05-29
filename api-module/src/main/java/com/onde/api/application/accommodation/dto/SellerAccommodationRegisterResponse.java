package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.accommodation.ApprovalStatus;

public record SellerAccommodationRegisterResponse(
    Long accommodationId,
    ApprovalStatus approvalStatus, // 등록 직후이므로 주로 PENDING(대기) 상태 반환
    String message
) {}