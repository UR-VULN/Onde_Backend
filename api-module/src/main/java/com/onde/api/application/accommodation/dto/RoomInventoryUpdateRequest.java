package com.onde.api.application.accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class RoomInventoryUpdateRequest {
    
    @NotNull(message = "객실 ID는 필수 입력값입니다.")
    private Long roomId;

    @NotNull(message = "재고 수정 날짜는 필수 입력값입니다.")
    private LocalDate date;

    @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
    private Integer stock;

    @Min(value = 0, message = "기본 가격은 0원 이상이어야 합니다.")
    private BigDecimal basePrice;
}
