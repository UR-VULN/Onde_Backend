package com.onde.admin.application.approval.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminApprovalRequest {
    private String action; // APPROVE, REJECT
    private String reason;

    private String category; // FLIGHT, INSURANCE
    private ApprovalStatus decision; // APPROVED, REJECTED
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
        return ApprovalStatus.valueOf(normalized);
    }

    public String getResolvedRejectReason() {
        return reason != null ? reason : rejectReason;
    }
}
