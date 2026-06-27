package com.onde.admin.application.approval.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApprovalProcessRequest(
        @Size(max = 50, message = "approvalType은 50자 이하여야 합니다.")
        String approvalType,
        @NotNull(message = "targetId는 필수입니다.")
        @Positive(message = "targetId는 양수여야 합니다.")
        Long targetId,
        @Size(max = 30, message = "action 값은 30자 이하여야 합니다.")
        String action,
        @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "반려 사유는 500자 이하여야 합니다.")
        String rejectReason,
        @Size(max = 30, message = "status 값은 30자 이하여야 합니다.")
        String status,
        @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "메모는 500자 이하여야 합니다.")
        String adminMemo
) {
}
