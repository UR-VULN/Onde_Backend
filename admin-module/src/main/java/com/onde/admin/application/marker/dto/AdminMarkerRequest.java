package com.onde.admin.application.marker.dto;

import com.onde.core.entity.lbs.MarkerCategory;
import jakarta.validation.constraints.DecimalMax; 
import jakarta.validation.constraints.DecimalMin; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMarkerRequest {

    @NotBlank(message = "관광지/맛집 상호명은 필수입니다.")
    private String name;

    @NotNull(message = "카테고리는 필수입니다.")
    private MarkerCategory category;

    @NotNull(message = "위도 좌표는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "올바른 위도 값을 입력하세요 (-90 ~ 90).")
    @DecimalMax(value = "90.0", message = "올바른 위도 값을 입력하세요 (-90 ~ 90).")
    private Double latitude;

    @NotNull(message = "경도 좌표는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "올바른 경도 값을 입력하세요 (-180 ~ 180).")
    @DecimalMax(value = "180.0", message = "올바른 경도 값을 입력하세요 (-180 ~ 180).")
    private Double longitude;
}
