package com.onde.api.application.lbs.dto;

import com.onde.core.entity.lbs.Property;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyMarkerDto {
    private Long propertyId;
    private String addressName;
    private Double latitude;
    private Double longitude;
    private Long memberId;

    public static PropertyMarkerDto from(Property property) {
        return PropertyMarkerDto.builder()
                .propertyId(property.getId())
                .addressName(property.getAddressName())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .memberId(property.getSellerId())
                .build();
    }
}

