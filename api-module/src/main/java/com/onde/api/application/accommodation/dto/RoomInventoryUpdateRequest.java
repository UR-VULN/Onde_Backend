package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class RoomInventoryUpdateRequest {
    private LocalDate date;
    private Integer stock;
    private BigDecimal basePrice;
}
