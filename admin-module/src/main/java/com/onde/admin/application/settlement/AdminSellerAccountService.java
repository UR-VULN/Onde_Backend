package com.onde.admin.application.settlement;

import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.repository.SellerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSellerAccountService {
    private final SellerAccountRepository sellerAccountRepository;

    @Transactional
    public void approveAccount(Long settlementId) {
        SellerAccount account = sellerAccountRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 계좌 정보를 찾을 수 없습니다."));
        
        account.approveAccount(); // 엔티티 비즈니스 메서드 호출
    }

    @Transactional
    public void rejectAccount(Long settlementId) {
        SellerAccount account = sellerAccountRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 계좌 정보를 찾을 수 없습니다."));
        
        account.rejectAccount();
    }
}
