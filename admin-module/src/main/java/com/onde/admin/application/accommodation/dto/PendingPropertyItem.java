package com.onde.admin.application.accommodation.dto;

public record PendingPropertyItem(
        Long id,
        String type,
        String name,
        String approvalStatus,
        Long sellerId
) {
}
