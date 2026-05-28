package com.onde.core.repository;

import com.onde.core.entity.settlement.SellerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerAccountRepository extends JpaRepository<SellerAccount, Long> {
    Optional<SellerAccount> findByMemberId(Long memberId);
}