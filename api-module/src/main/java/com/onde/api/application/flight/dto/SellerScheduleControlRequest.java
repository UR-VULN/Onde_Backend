package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerScheduleControlRequest {

    private SeatClass seatClass;
    private SeatClass classType;

    @DecimalMin(value = "0", message = "가격은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "가격이 허용 범위를 초과합니다.")
    private BigDecimal newPrice;

    @DecimalMin(value = "0", message = "가격은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "가격이 허용 범위를 초과합니다.")
    private BigDecimal overridePrice;

    @Min(value = 0, message = "잔여 좌석은 0 이상이어야 합니다.")
    @Max(value = 9999, message = "잔여 좌석이 허용 범위를 초과합니다.")
    private Integer availableSeats;

    @Min(value = 0, message = "잔여 좌석은 0 이상이어야 합니다.")
    @Max(value = 9999, message = "잔여 좌석이 허용 범위를 초과합니다.")
    private Integer remainingSeats;

    @Size(max = 30, message = "controlType 형식이 올바르지 않습니다.")
    private String controlType;

    public SeatClass getEffectiveSeatClass() {
        return seatClass != null ? seatClass : classType;
    }

    public Integer getEffectiveAvailableSeats() {
        return availableSeats != null ? availableSeats : remainingSeats;
    }

    public BigDecimal getEffectiveNewPrice() {
        return newPrice != null ? newPrice : overridePrice;
    }
}
