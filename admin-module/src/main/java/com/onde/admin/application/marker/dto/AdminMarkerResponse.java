package com.onde.admin.application.marker.dto;

import com.onde.core.entity.lbs.GuideMarker;
import com.onde.core.entity.lbs.MarkerCategory;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMarkerResponse {
    private Long markerId;
    private String name;
    private MarkerCategory category;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;

    public static AdminMarkerResponse from(GuideMarker marker) {
        return AdminMarkerResponse.builder()
                .markerId(marker.getMarkerId())
                .name(marker.getName())
                .category(marker.getCategory())
                .latitude(marker.getLatitude())
                .longitude(marker.getLongitude())
                .createdAt(marker.getCreatedAt())
                .build();
    }
}
