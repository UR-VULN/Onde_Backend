package com.onde.api.application.community;

import com.onde.api.application.community.dto.PostLikeToggleResponse;
import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostLike;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PostLikeRepository;
import com.onde.core.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PostLikeToggleResponse toggleLike(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberId);
        boolean isLiked;

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            isLiked = false;
            log.info("Post like toggled OFF. postId={}, memberId={}", postId, memberId);
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .member(member)
                    .build();
            postLikeRepository.save(newLike);
            post.incrementLikeCount();
            isLiked = true;
            log.info("Post like toggled ON. postId={}, memberId={}", postId, memberId);
        }

        return PostLikeToggleResponse.builder()
                .postId(postId)
                .isLiked(isLiked)
                .likeCount(post.getLikeCount())
                .build();
    }
}
