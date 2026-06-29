package com.onde.api.application.community;

import com.onde.api.application.community.dto.CommentDto;
import com.onde.api.application.community.dto.CommentRequest;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @PathVariable("postId") Long postId,
            @RequestBody CommentRequest req,
            @LoginMember Long memberId) {

        CommentDto response = commentService.createComment(postId, req, memberId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "댓글이 등록되었습니다."));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(
            @PathVariable("postId") Long postId,
            @LoginMember(required = false) Long memberId) {

        List<CommentDto> response = commentService.getComments(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto>> updateComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequest req,
            @LoginMember Long memberId) {

        CommentDto response = commentService.updateComment(commentId, req, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다."));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("commentId") Long commentId,
            @LoginMember Long memberId) {

        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다."));
    }
}
