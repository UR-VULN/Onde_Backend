package com.onde.api.application.community;

import com.onde.api.application.community.dto.PostCreateRequest;
import com.onde.api.application.community.dto.PostCreateResponse;
import com.onde.api.application.community.dto.PostDeleteResponse;
import com.onde.api.application.community.dto.PostSearchResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import com.onde.core.support.ApiResponse;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @Valid @ModelAttribute PostCreateRequest req,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @LoginMember Long memberId) {

        PostCreateResponse response = postService.createPost(req, images, memberId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "게시글이 등록되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PostSearchResponse>> getPosts(
            @RequestParam(value = "type", required = false) PostType type,
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") PostStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Pageable pageable = PageRequest.of(page, size);
        PostSearchResponse response = postService.getPosts(type, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDeleteResponse>> deletePost(
            @PathVariable("postId") @Min(1) Long postId,
            @LoginMember Long memberId) {

        PostDeleteResponse response = postService.deletePost(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 삭제되었습니다."));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponse>> updatePost(
            @PathVariable("postId") @Min(1) Long postId,
            @Valid @ModelAttribute PostCreateRequest req,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @LoginMember Long memberId) {

        PostCreateResponse response = postService.updatePost(postId, req, images, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 수정되었습니다."));
    }
}
