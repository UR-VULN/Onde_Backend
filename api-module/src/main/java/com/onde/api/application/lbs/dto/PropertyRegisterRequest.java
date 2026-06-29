package com.onde.api.application.lbs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRegisterRequest {

    @NotBlank(message = "주소명은 필수입니다.")
    private String addressName;

    @NotNull(message = "위도 좌표는 필수입니다.")
    private Double latitude;

    @NotNull(message = "경도 좌표는 필수입니다.")
    private Double longitude;
}
