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
    private String category; // FLIGHT, INSURANCE
    private ApprovalStatus decision; // APPROVED, REJECTED
    private String rejectReason;
}
