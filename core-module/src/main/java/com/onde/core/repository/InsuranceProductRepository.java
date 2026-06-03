package com.onde.core.repository;

import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.entity.insurance.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByStatus(ApprovalStatus status);
}
