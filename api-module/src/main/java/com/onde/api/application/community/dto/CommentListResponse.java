package com.onde.api.application.community.dto;

import java.util.List;

public record CommentListResponse(
        List<CommentDto> comments,
        Integer totalCount
) {
}
