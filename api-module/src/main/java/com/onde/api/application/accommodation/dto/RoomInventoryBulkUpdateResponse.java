package com.onde.api.application.accommodation.dto;

import java.time.LocalDate;
import java.util.List;

public record RoomInventoryBulkUpdateResponse(
        Long roomId,
        List<LocalDate> updatedDates,
        Integer updatedCount
) {
}
