package com.onde.core.repository;

import com.onde.core.entity.community.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPostIdOrderBySortOrderAsc(Long postId);
    List<PostImage> findByPostId(Long postId);
    void deleteByPostId(Long postId);
}
