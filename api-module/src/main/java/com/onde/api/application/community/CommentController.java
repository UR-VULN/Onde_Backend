package com.onde.api.application.community;

import com.onde.api.application.community.dto.CommentDto;
import com.onde.api.application.community.dto.CommentRequest;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @PathVariable("postId") @Min(1) Long postId,
            @Valid @RequestBody CommentRequest req,
            @LoginMember Long memberId) {

        CommentDto response = commentService.createComment(postId, req, memberId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "댓글이 등록되었습니다."));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(
            @PathVariable("postId") @Min(1) Long postId,
            @LoginMember(required = false) Long memberId) {

        List<CommentDto> response = commentService.getComments(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto>> updateComment(
            @PathVariable("commentId") @Min(1) Long commentId,
            @Valid @RequestBody CommentRequest req,
            @LoginMember Long memberId) {

        CommentDto response = commentService.updateComment(commentId, req, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다."));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("commentId") @Min(1) Long commentId,
            @LoginMember Long memberId) {

        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다."));
    }
}
