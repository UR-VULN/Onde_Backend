package com.onde.api.application.lbs.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySearchResponse {
    private List<PropertyMarkerDto> markers;
    private int totalCount;
}
