package com.onde.api.application.community;

import com.onde.api.application.community.dto.PostCreateRequest;
import com.onde.api.application.community.dto.PostCreateResponse;
import com.onde.api.application.community.dto.PostDeleteResponse;
import com.onde.api.application.community.dto.PostSearchResponse;
import com.onde.api.application.community.dto.PostDto;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("type") PostType type,
            @RequestParam(value = "rating", required = false, defaultValue = "5") Integer rating,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @LoginMember Long memberId) {

        PostCreateRequest req = PostCreateRequest.builder()
                .title(title)
                .content(content)
                .type(type)
                .rating(rating)
                .build();

        PostCreateResponse response = postService.createPost(req, images, memberId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "게시글이 등록되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PostSearchResponse>> getPosts(
            @RequestParam(value = "type", required = false) PostType type,
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") PostStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        PostSearchResponse response = postService.getPosts(type, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDeleteResponse>> deletePost(
            @PathVariable("postId") Long postId,
            @LoginMember Long memberId) {

        PostDeleteResponse response = postService.deletePost(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 삭제되었습니다."));
    }
}
