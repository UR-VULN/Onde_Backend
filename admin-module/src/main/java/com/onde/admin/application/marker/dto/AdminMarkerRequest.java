package com.onde.admin.application.marker.dto;

import com.onde.core.entity.lbs.MarkerCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMarkerRequest {

    @NotBlank(message = "관광지/맛집 상호명은 필수입니다.")
    @Size(max = 100, message = "상호명은 100자를 초과할 수 없습니다.")
    private String name;

    @NotNull(message = "카테고리는 필수입니다.")
    private MarkerCategory category;

    @NotNull(message = "위도 좌표는 필수입니다.")
    private Double latitude;

    @NotNull(message = "경도 좌표는 필수입니다.")
    private Double longitude;

    @Size(max = 500, message = "URL은 500자를 초과할 수 없습니다.")
    @URL(message = "올바른 URL 형식이어야 합니다.")
    private String url;
}
