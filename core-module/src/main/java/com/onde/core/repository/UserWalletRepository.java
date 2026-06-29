package com.onde.core.repository;

import com.onde.core.entity.payment.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByMemberId(Long memberId);
}
