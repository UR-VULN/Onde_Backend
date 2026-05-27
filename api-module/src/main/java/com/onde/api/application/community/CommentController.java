package com.onde.api.application.community;

import com.onde.api.application.community.dto.CommentCreateRequest;
import com.onde.api.application.community.dto.CommentDeleteResponse;
import com.onde.api.application.community.dto.CommentDto;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody CommentCreateRequest req,
            @LoginMember Long memberId) {

        CommentDto response = commentService.createComment(postId, req, memberId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "댓글이 성공적으로 등록되었습니다."));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(
            @PathVariable("postId") Long postId) {

        List<CommentDto> response = commentService.getComments(postId);
        return ResponseEntity.ok(ApiResponse.success(response, "댓글 목록 조회가 완료되었습니다."));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDeleteResponse>> deleteComment(
            @PathVariable("commentId") Long commentId,
            @LoginMember Long memberId) {

        CommentDeleteResponse response = commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "댓글이 성공적으로 삭제되었습니다."));
    }
}
