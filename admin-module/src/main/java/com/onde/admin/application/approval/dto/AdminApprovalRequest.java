package com.onde.admin.application.approval.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminApprovalRequest {
    private String action; // APPROVE, REJECT
    
    @Size(max = 500, message = "반려 사유는 500자를 초과할 수 없습니다.")
    private String reason;

    private String category; // FLIGHT, INSURANCE
    private ApprovalStatus decision; // APPROVED, REJECTED
    
    @Size(max = 500, message = "반려 사유는 500자를 초과할 수 없습니다.")
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

