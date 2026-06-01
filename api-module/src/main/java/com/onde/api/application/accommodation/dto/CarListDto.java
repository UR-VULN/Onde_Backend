package com.onde.api.application.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CarListDto {
    private Long id;
    private String modelName;
    private String carType;
    private Integer price;
}
