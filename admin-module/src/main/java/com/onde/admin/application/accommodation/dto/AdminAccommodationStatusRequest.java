package com.onde.admin.application.accommodation.dto;

import com.onde.core.entity.accommodation.ApprovalStatus;

public record AdminAccommodationStatusRequest(
        ApprovalStatus approvalStatus,
        String rejectReason
) {
}
