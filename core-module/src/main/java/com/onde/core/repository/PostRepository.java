package com.onde.core.repository;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 타입 필터 + ACTIVE 상태만 페이징 조회
    Page<Post> findByTypeAndStatus(PostType type, PostStatus status, Pageable pageable);

    // 타입 없이 ACTIVE만 조회 (전체 조회용)
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    long countByStatus(PostStatus status);
}

