package com.onde.core.repository;

import com.onde.core.entity.insurance.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    org.springframework.data.domain.Page<InsurancePolicy> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<InsurancePolicy> findByUserIdAndStatus(Long userId, com.onde.core.entity.insurance.InsurancePolicyStatus status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<InsurancePolicy> findByUserIdAndStatusIn(Long userId, java.util.Collection<com.onde.core.entity.insurance.InsurancePolicyStatus> statuses, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(ip.totalPremium), 0) FROM InsurancePolicy ip WHERE ip.status != 'CANCELLED' AND ip.createdAt >= :start AND ip.createdAt < :end")
    java.math.BigDecimal sumTotalPremiumByStatusNotAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(ip) FROM InsurancePolicy ip WHERE ip.status != 'CANCELLED' AND ip.createdAt >= :start AND ip.createdAt < :end")
    long countByStatusNotAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );
}

