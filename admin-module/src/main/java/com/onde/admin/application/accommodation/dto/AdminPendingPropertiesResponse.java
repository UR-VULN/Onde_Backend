package com.onde.admin.application.accommodation.dto;

import java.util.List;

public record AdminPendingPropertiesResponse(
        List<PendingPropertyItem> items,
        Integer totalCount
) {
}
