package com.onde.admin.application.approval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onde.core.entity.flight.ApprovalStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminApprovalResponse {
    private Long requestId;
    private ApprovalStatus status;
    private LocalDateTime processedAt;

    private ApprovalStatus decision;
    private LocalDateTime updatedAt;
}
