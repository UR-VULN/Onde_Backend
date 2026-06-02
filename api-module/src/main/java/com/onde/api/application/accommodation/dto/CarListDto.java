package com.onde.api.application.accommodation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarListDto {
    private Long carId;
    private Long id;
    private String modelName;
    private String carType;
    private String licensePlate;
    private Integer dailyPrice;
    private Integer price;
    private Boolean available;
}
