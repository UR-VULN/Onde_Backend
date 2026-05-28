package com.onde.admin.application.approval.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminApprovalResponse {
    private Long requestId;
    private ApprovalStatus decision;
    private LocalDateTime updatedAt;
}
