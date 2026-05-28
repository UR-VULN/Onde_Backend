package com.onde.api.application.community;

import com.onde.api.application.community.dto.CommentCreateRequest;
import com.onde.api.application.community.dto.CommentDeleteResponse;
import com.onde.api.application.community.dto.CommentDto;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentDto createComment(Long postId, CommentCreateRequest req, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(req.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);
        post.incrementCommentCount();

        log.info("Comment created successfully. commentId={}, postId={}, memberId={}", savedComment.getId(), postId, memberId);
        return CommentDto.of(savedComment);
    }

    public List<CommentDto> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException(ErrorCode.POST_NOT_FOUND);
        }

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream().map(CommentDto::of).toList();
    }

    @Transactional
    public CommentDeleteResponse deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 작성자 소유권 및 권한 검증
        if (!comment.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.COMMENT_NOT_OWNER);
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);
        post.decrementCommentCount();

        log.info("Comment deleted successfully. commentId={}, postId={}, memberId={}", commentId, post.getId(), memberId);
        return CommentDeleteResponse.builder()
                .commentId(commentId)
                .build();
    }
}
