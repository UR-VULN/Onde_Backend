package com.onde.core.repository;

import com.onde.core.entity.insurance.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    org.springframework.data.domain.Page<InsurancePolicy> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<InsurancePolicy> findByUserIdAndStatus(Long userId, String status, org.springframework.data.domain.Pageable pageable);
}
