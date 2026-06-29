package com.onde.admin.application.accommodation.dto;

import com.onde.core.entity.accommodation.ApprovalStatus;
import java.time.LocalDateTime;

public record AdminAccommodationStatusResponse(
        Long accommodationId,
        ApprovalStatus approvalStatus,
        LocalDateTime processedAt
) {
}
