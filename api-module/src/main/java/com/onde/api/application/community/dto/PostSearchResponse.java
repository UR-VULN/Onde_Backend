package com.onde.api.application.community.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSearchResponse {
    private List<PostDto> posts;
    private long totalCount;
}
