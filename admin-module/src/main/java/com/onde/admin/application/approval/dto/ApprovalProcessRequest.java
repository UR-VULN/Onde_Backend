package com.onde.admin.application.approval.dto;

public record ApprovalProcessRequest(
    String approvalType,
    Long targetId,
    String action,
    String rejectReason,
    String status,
    String adminMemo
) {}
