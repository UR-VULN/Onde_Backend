package com.onde.core.repository;

import com.onde.core.entity.community.Post;
import java.util.List;

public interface PostRepositoryCustom {
    List<Post> findByTypeAndStatus(String type, String status);
    List<Post> findByStatus(String status);
    long countByStatus(String status);
}
