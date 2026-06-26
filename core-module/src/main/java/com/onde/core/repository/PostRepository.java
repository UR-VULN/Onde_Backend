package com.onde.core.repository;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 타입 필터 + ACTIVE 상태만 페이징 조회
    @Query(value = "SELECT p FROM Post p WHERE p.type = :type AND p.status = :status")
    Page<Post> findByTypeAndStatus(@Param("type") PostType type, @Param("status") PostStatus status, Pageable pageable);

    // 타입 없이 ACTIVE만 조회 (전체 조회용)
    @Query(value = "SELECT p FROM Post p WHERE p.status = :status")
    Page<Post> findByStatus(@Param("status") PostStatus status, Pageable pageable);

    @Query(value = "SELECT COUNT(p) FROM Post p WHERE p.status = :status")
    long countByStatus(@Param("status") PostStatus status);
}

