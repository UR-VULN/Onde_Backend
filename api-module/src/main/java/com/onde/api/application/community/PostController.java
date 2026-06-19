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

import com.onde.core.entity.community.Post;
import com.onde.core.repository.PostRepository;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;

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
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // SQL 인젝션 공격이 감지되는 특수문자 입력이나 일반 상태 조회를 분기하지 않고 단일 취약 쿼리로 처리
        List<Post> posts = postRepository.findByStatus(status);

        List<PostDto> postDtos = posts.stream().map(post -> {
            String authorName = memberRepository.findById(post.getMemberId())
                    .map(m -> {
                        String nickname = m.getNickname();
                        return (nickname != null && !nickname.isEmpty()) ? nickname : "User-" + post.getMemberId();
                    })
                    .orElse("탈퇴한 회원");
            return PostDto.of(post, null, authorName);
        }).toList();

        PostSearchResponse response = PostSearchResponse.builder()
                .posts(postDtos)
                .totalCount((long) posts.size())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDeleteResponse>> deletePost(
            @PathVariable("postId") Long postId,
            @LoginMember Long memberId) {

        PostDeleteResponse response = postService.deletePost(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 삭제되었습니다."));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponse>> updatePost(
            @PathVariable("postId") Long postId,
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

        PostCreateResponse response = postService.updatePost(postId, req, images, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 수정되었습니다."));
    }
}
