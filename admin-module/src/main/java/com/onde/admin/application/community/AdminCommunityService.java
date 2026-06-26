package com.onde.admin.application.community;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.onde.admin.application.community.dto.AdminBlindRequest;
import com.onde.admin.application.community.dto.AdminBlindResponse;
import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.FcmTokenRepository;
import com.onde.core.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final com.onde.core.repository.PostImageRepository postImageRepository;
    private final com.onde.core.repository.MemberRepository memberRepository;

    public AdminCommunityService(
            PostRepository postRepository,
            FcmTokenRepository fcmTokenRepository,
            FirebaseMessaging firebaseMessaging,
            com.onde.core.repository.PostImageRepository postImageRepository,
            com.onde.core.repository.MemberRepository memberRepository) {
        this.postRepository = postRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.firebaseMessaging = firebaseMessaging;
        this.postImageRepository = postImageRepository;
        this.memberRepository = memberRepository;
    }

    public org.springframework.data.domain.Page<com.onde.admin.application.community.dto.AdminPostDetailResponse> getAdminPosts(
            PostStatus status, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Post> posts = (status != null)
                ? postRepository.findByStatus(status, pageable)
                : postRepository.findAll(pageable);

        return posts.map(post -> {
            List<String> imageUrls = postImageRepository.findByPostIdOrderBySortOrderAsc(post.getId())
                    .stream().map(com.onde.core.entity.community.PostImage::getImageUrl).toList();
            String authorName = memberRepository.findById(post.getMemberId())
                    .map(m -> {
                        String nickname = m.getNickname();
                        return (nickname != null && !nickname.isEmpty()) ? nickname : "User-" + post.getMemberId();
                    })
                    .orElse("탈퇴한 회원");
            return com.onde.admin.application.community.dto.AdminPostDetailResponse.of(post, imageUrls, authorName);
        });
    }

    public com.onde.admin.application.community.dto.AdminPostDetailResponse getAdminPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        List<String> imageUrls = postImageRepository.findByPostIdOrderBySortOrderAsc(post.getId())
                .stream().map(com.onde.core.entity.community.PostImage::getImageUrl).toList();

        String authorName = memberRepository.findById(post.getMemberId())
                .map(m -> {
                    String nickname = m.getNickname();
                    return (nickname != null && !nickname.isEmpty()) ? nickname : "User-" + post.getMemberId();
                })
                .orElse("탈퇴한 회원");

        return com.onde.admin.application.community.dto.AdminPostDetailResponse.of(post, imageUrls, authorName);
    }

    @Transactional
    public void forceDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        post.updateStatus(PostStatus.DELETED);
    }

    @Transactional
    public AdminBlindResponse blindPost(Long postId, AdminBlindRequest req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        // 1. 블라인드 처리 (BLINDED)
        post.updateStatus(PostStatus.BLINDED);

        // 2. 작성자 회원 FCM 알림 발송 (단건 푸시 발송)
        Long authorId = post.getMemberId();
        String title = "게시글 블라인드 안내";
        String body = String.format("작성하신 게시글이 블라인드 처리되었습니다. (사유: %s)", req.getReason());
        sendSinglePush(authorId, title, body);

        return AdminBlindResponse.builder()
                .postId(postId)
                .status(PostStatus.BLINDED)
                .blindedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public AdminBlindResponse restorePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        // 1. 복구 처리 (ACTIVE)
        post.updateStatus(PostStatus.ACTIVE);

        // 2. 작성자 회원 FCM 알림 발송 (단건 푸시 발송)
        Long authorId = post.getMemberId();
        String title = "게시글 복구 안내";
        String body = "블라인드되었던 게시글이 정상 복구되어 노출이 재개되었습니다.";
        sendSinglePush(authorId, title, body);

        return AdminBlindResponse.builder()
                .postId(postId)
                .status(PostStatus.ACTIVE)
                .blindedAt(LocalDateTime.now())
                .build();
    }

    private void sendSinglePush(Long memberId, String title, String body) {
        List<FcmToken> tokens = fcmTokenRepository.findByMemberId(memberId);

        tokens.forEach(token -> {
            Message message = Message.builder()
                    .setToken(token.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            try {
                firebaseMessaging.send(message);
                log.info("Admin FCM push sent successfully to memberId={}", memberId);
            } catch (Exception e) {
                log.warn("Admin FCM push failed for token={}: {}", token.getFcmToken(), e.getMessage());
            }
        });
    }
}
