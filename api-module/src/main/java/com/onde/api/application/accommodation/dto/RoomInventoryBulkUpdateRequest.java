package com.onde.api.application.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RoomInventoryBulkUpdateRequest(
        @NotNull Long roomId,
        @NotNull @Valid List<UpdateItem> updates
) {
    public record UpdateItem(
            @NotNull LocalDate date,
            @Min(0) BigDecimal basePrice,
            @Min(0) Integer stock
    ) {
    }
}
