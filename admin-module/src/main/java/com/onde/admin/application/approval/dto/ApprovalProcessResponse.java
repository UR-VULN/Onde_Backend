package com.onde.admin.application.approval.dto;

import java.time.LocalDateTime;

public record ApprovalProcessResponse(
    String approvalType,
    Long targetId,
    String status,
    LocalDateTime processedAt
) {}
