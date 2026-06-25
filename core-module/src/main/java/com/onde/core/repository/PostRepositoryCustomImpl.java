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
        String sql = "SELECT * FROM posts WHERE type = :type AND status = :status";
        return em.createNativeQuery(sql, Post.class)
                .setParameter("type", type)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Post> findByStatus(String status) {
        String sql = "SELECT * FROM posts WHERE status = :status";
        return em.createNativeQuery(sql, Post.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM posts WHERE status = :status";
        Object result = em.createNativeQuery(sql)
                .setParameter("status", status)
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
