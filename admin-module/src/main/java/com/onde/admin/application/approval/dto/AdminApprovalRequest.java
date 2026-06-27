package com.onde.admin.application.approval.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminApprovalRequest {

    @Size(max = 30, message = "action 값은 30자 이하여야 합니다.")
    private String action;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "사유는 500자 이하여야 합니다.")
    private String reason;

    @Size(max = 50, message = "category 값은 50자 이하여야 합니다.")
    private String category;

    private ApprovalStatus decision;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "반려 사유는 500자 이하여야 합니다.")
    private String rejectReason;

    public ApprovalStatus getResolvedDecision() {
        if (decision != null) {
            return decision;
        }
        if (action == null || action.isBlank()) {
            return null;
        }
        String normalized = action.trim().toUpperCase();
        if (normalized.equals("APPROVE")) {
            return ApprovalStatus.APPROVED;
        }
        if (normalized.equals("REJECT")) {
            return ApprovalStatus.REJECTED;
        }
        try {
            return ApprovalStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getResolvedRejectReason() {
        return reason != null ? reason : rejectReason;
    }
}
