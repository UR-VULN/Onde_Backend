package com.onde.api.application.flight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerScheduleControlResponse {
    private Long scheduleId;
    private SeatClass seatClass;
    private BigDecimal updatedPrice;
    private Integer updatedSeats;

    private SeatClass classType;
    private Integer remainingSeats;
    private BigDecimal currentPrice;
}
