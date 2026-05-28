package com.onde.admin.application.approval.dto;

import com.onde.core.entity.accommodation.ApprovalStatus;

// 처리 결과를 클라이언트에게 반환하기 위한 record
public record ApprovalProcessResponse(
    Long targetId,
    ApprovalStatus status,
    String message
) {}