package com.onde.admin.application.settlement.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminSettlementRejectRequest {

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "반려 사유는 500자 이하여야 합니다.")
    private String rejectReason;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "반려 사유는 500자 이하여야 합니다.")
    private String comment;

    public String resolveReason() {
        if (rejectReason != null && !rejectReason.isBlank()) {
            return rejectReason;
        }
        return comment;
    }
}
