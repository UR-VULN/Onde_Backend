package com.onde.api.application.community;

import com.onde.api.application.community.dto.CommentDto;
import com.onde.api.application.community.dto.CommentRequest;
import com.onde.api.application.notification.NotificationService;
import com.onde.core.entity.community.Comment;
import com.onde.core.entity.community.Post;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.CommentRepository;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentDto createComment(Long postId, CommentRequest req, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        Comment comment = Comment.builder()
                .postId(postId)
                .memberId(memberId)
                .content(HtmlUtils.htmlEscape(req.getContent())) // XSS 방지
                .isSecret(req.getIsSecret() != null ? req.getIsSecret() : false)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 게시글 댓글 수 증가
        post.incrementCommentCount();
        postRepository.save(post);

        // 원글 작성자에게 알림 발송 (댓글 작성자와 원글 작성자가 다를 때만)
        if (!post.getMemberId().equals(memberId)) {
            try {
                String title = "새 댓글 알림";
                String body = "내 게시글 [" + post.getTitle() + "]에 새로운 댓글이 달렸습니다.";
                notificationService.sendSinglePush(post.getMemberId(), title, body);
            } catch (Exception e) {
                log.warn("댓글 등록 알림 발송 실패: {}", e.getMessage());
            }
        }

        String authorName = member.getNickname();
        if (authorName == null || authorName.isEmpty()) {
            authorName = "User-" + memberId;
        }

        return CommentDto.of(savedComment, authorName, savedComment.getContent());
    }

    public List<CommentDto> getComments(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        return comments.stream().map(comment -> {
            String authorName = memberRepository.findById(comment.getMemberId())
                    .map(m -> {
                        String nickname = m.getNickname();
                        return (nickname != null && !nickname.isEmpty()) ? nickname : "User-" + comment.getMemberId();
                    })
                    .orElse("탈퇴한 회원");

            String displayContent = comment.getContent();

            // 비밀댓글 열람 권한 검증: 비밀글인 경우 댓글 작성자 본인 혹은 원본 게시글 작성자만 볼 수 있음
            if (comment.getIsSecret()) {
                boolean hasPermission = memberId != null && 
                        (comment.getMemberId().equals(memberId) || post.getMemberId().equals(memberId));
                if (!hasPermission) {
                    displayContent = "비밀 댓글입니다.";
                }
            }

            return CommentDto.of(comment, authorName, displayContent);
        }).toList();
    }

    @Transactional
    public CommentDto updateComment(Long commentId, CommentRequest req, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND)); // 간단한 예외 처리

        if (!comment.getMemberId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.POST_NOT_OWNER); // 권한 없음 예외
        }

        comment.updateContent(HtmlUtils.htmlEscape(req.getContent()), req.getIsSecret() != null ? req.getIsSecret() : false); // XSS 방지
        Comment updatedComment = commentRepository.save(comment);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        String authorName = member.getNickname();
        if (authorName == null || authorName.isEmpty()) {
            authorName = "User-" + memberId;
        }

        return CommentDto.of(updatedComment, authorName, updatedComment.getContent());
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        if (!comment.getMemberId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.POST_NOT_OWNER);
        }

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        commentRepository.delete(comment);

        post.decrementCommentCount();
        postRepository.save(post);
    }
}
