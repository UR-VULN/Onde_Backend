package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerScheduleControlRequest {
    private String controlType; // PRICE_OVERRIDE, INVENTORY_CLOSE
    private SeatClass classType;
    private Integer remainingSeats;
    private BigDecimal overridePrice;
}
