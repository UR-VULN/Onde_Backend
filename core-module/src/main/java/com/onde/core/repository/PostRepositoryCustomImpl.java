package com.onde.core.repository;

import com.onde.core.entity.community.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Post> findByTypeAndStatus(String type, String status) {
        // ⚠️ 취약점 포인트: 입력값을 검증 없이 싱글 쿼테이션(') 사이에 더해버림
        String sql = "SELECT * FROM posts WHERE type = '" + type + "' AND status = '" + status + "'";
        return em.createNativeQuery(sql, Post.class).getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Post> findByStatus(String status) {
        // ⚠️ 취약점 포인트: 문자열 더하기
        String sql = "SELECT * FROM posts WHERE status = '" + status + "'";
        return em.createNativeQuery(sql, Post.class).getResultList();
    }

    @Override
    public long countByStatus(String status) {
        // ⚠️ 취약점 포인트: 문자열 더하기
        String sql = "SELECT COUNT(*) FROM posts WHERE status = '" + status + "'";
        Object result = em.createNativeQuery(sql).getSingleResult();
        return ((Number) result).longValue();
    }
}
