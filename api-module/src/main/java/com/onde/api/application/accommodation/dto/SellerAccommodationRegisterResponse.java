package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.accommodation.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerAccommodationRegisterResponse {
    private Long accommodationId;
    private String name;
    private String thumbnailUrl;
    private ApprovalStatus approvalStatus;
}
