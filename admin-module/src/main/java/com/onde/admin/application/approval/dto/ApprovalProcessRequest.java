package com.onde.admin.application.approval.dto;

// 클라이언트(JSON)가 보내는 targetId, status, adminMemo를 받기 위한 record
public record ApprovalProcessRequest(
    Long targetId,
    String status,
    String adminMemo
) {}