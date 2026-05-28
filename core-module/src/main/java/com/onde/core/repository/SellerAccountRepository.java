package com.onde.core.repository;

import com.onde.core.entity.settlement.SellerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * SellerAccount 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface SellerAccountRepository extends JpaRepository<SellerAccount, Long> {

    /**
     * 특정 판매자(sellerId)의 정산 계좌 정보를 조회합니다.
     *
     * @param sellerId 조회할 판매자 식별자
     * @return 조회된 판매자 계좌 정보 (존재할 경우)
     */
    Optional<SellerAccount> findBySellerId(Long sellerId);
}

