package com.onde.api.application.lbs.dto;

import com.onde.core.entity.lbs.Property;
import lombok.*;
import java.time.LocalDateTime;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRegisterResponse {
    private Long propertyId;
    private String addressName;
    private Double latitude;
    private Double longitude;
    private boolean isVerified;
    private LocalDateTime registeredAt;

    public static PropertyRegisterResponse from(Property property) {
        return PropertyRegisterResponse.builder()
                .propertyId(property.getId())
                .addressName(property.getAddressName())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .isVerified(property.getIsVerified())
                .registeredAt(property.getRegisteredAt())
                .build();
    }

    // Lombok 미인식 컴파일러 대비 수동 Getter 정의 (Jackson 직렬화 완벽 보장)
    public Long getPropertyId() {
        return propertyId;
    }

    public String getAddressName() {
        return addressName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
