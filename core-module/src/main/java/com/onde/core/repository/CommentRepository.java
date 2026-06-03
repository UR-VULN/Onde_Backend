package com.onde.core.repository;

import com.onde.core.entity.community.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 특정 게시글의 댓글을 등록 시간 순서로 오름차순 조회
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
