package com.onde.api.application.dashboard;

import com.onde.api.application.dashboard.dto.DashboardResponse;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.repository.SellerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final SellerAccountRepository sellerAccountRepository;

    public DashboardResponse getDashboardInfo(Member member) {
        SellerAccount sellerAccount = sellerAccountRepository.findByMemberId(member.getId())
                .orElse(null);

        return DashboardResponse.of(member, sellerAccount);
    }
}
