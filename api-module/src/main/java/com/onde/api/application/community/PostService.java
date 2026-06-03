package com.onde.api.application.community;

import com.onde.api.application.community.dto.*;
import com.onde.api.config.MockS3Uploader;
import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostImage;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PostImageRepository;
import com.onde.core.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final MemberRepository memberRepository;
    private final MockS3Uploader s3Uploader;
    private final Executor imageUploadExecutor;

    public PostService(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            MemberRepository memberRepository,
            MockS3Uploader s3Uploader,
            @Qualifier("imageUploadExecutor") Executor imageUploadExecutor
    ) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.memberRepository = memberRepository;
        this.s3Uploader = s3Uploader;
        this.imageUploadExecutor = imageUploadExecutor;
    }

    @Transactional
    public PostCreateResponse createPost(PostCreateRequest req, List<MultipartFile> images, Long memberId) {
        // 1. 이미지 최대 3장 검증
        if (images != null && images.size() > 3) {
            throw new ValidationException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        // 2. 작성자 회원 검증 (논리 FK)
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 3. 게시글 저장
        Post post = Post.builder()
                .memberId(memberId)
                .title(req.getTitle())
                .content(req.getContent())
                .type(req.getType())
                .status(PostStatus.ACTIVE)
                .likeCount(0)
                .commentCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        // 4. S3 이미지 병렬 업로드 (CompletableFuture)
        List<String> imageUrls = uploadImagesParallel(images);

        // 5. PostImage 엔티티 리스트 저장
        List<PostImage> postImages = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            PostImage postImage = PostImage.builder()
                    .postId(savedPost.getId())
                    .imageUrl(imageUrls.get(i))
                    .sortOrder(i)
                    .build();
            postImages.add(postImage);
        }
        if (!postImages.isEmpty()) {
            postImageRepository.saveAll(postImages);
        }

        return PostCreateResponse.of(savedPost, imageUrls);
    }

    public PostSearchResponse getPosts(PostType type, PostStatus status, Pageable pageable) {
        // status = 'ACTIVE' 필수
        PostStatus queryStatus = (status != null) ? status : PostStatus.ACTIVE;

        Page<Post> postPage;
        if (type != null) {
            postPage = postRepository.findByTypeAndStatus(type, queryStatus, pageable);
        } else {
            postPage = postRepository.findByStatus(queryStatus, pageable);
        }

        List<PostDto> postDtos = postPage.getContent().stream().map(post -> {
            // 대표 썸네일 (sortOrder = 0) 이미지 가져오기
            List<PostImage> postImages = postImageRepository.findByPostIdOrderBySortOrderAsc(post.getId());
            String thumbnailUrl = postImages.isEmpty() ? null : postImages.get(0).getImageUrl();
            return PostDto.of(post, thumbnailUrl);
        }).toList();

        return PostSearchResponse.builder()
                .posts(postDtos)
                .totalCount(postPage.getTotalElements())
                .build();
    }

    @Transactional
    public PostDeleteResponse deletePost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        // 본인 소유권 검증
        if (!post.getMemberId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.POST_NOT_OWNER);
        }

        // Soft Delete (상태만 DELETED 변경)
        post.updateStatus(PostStatus.DELETED);

        return PostDeleteResponse.builder()
                .postId(postId)
                .status(PostStatus.DELETED)
                .build();
    }

    private List<String> uploadImagesParallel(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<String>> futures = images.stream()
                .map(image -> CompletableFuture.supplyAsync(
                        () -> s3Uploader.upload(image, "posts"),
                        imageUploadExecutor
                ))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
