package com.onde.api.application.community;

import com.onde.api.application.community.dto.PostLikeToggleResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeToggleResponse>> toggleLike(
            @PathVariable("postId") Long postId,
            @LoginMember Long memberId) {

        PostLikeToggleResponse response = postLikeService.toggleLike(postId, memberId);
        String message = response.isLiked() ? "게시글에 좋아요를 눌렀습니다." : "게시글 좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
