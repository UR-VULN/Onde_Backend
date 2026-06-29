package com.onde.admin.application.settlement;

import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
import com.onde.core.entity.payment.Payment;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SellerAccountRepository;
import com.onde.core.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final SettlementRepository settlementRepository;
    private final SellerAccountRepository sellerAccountRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 특정 판매자의 정산 계좌 조회
     */
    @Transactional(readOnly = true)
    public SellerAccount getAccount(Long sellerId) {
        return sellerAccountRepository.findByMemberId(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 계좌 없음"));
    }

    /**
     * 계좌번호 마스킹
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        if (accountNumber.contains("-")) {
            String[] parts = accountNumber.split("-");
            if (parts.length >= 3) {
                StringBuilder sb = new StringBuilder();
                sb.append(parts[0]).append("-");
                for (int i = 1; i < parts.length - 1; i++) {
                    sb.append("*".repeat(parts[i].length())).append("-");
                }
                sb.append(parts[parts.length - 1]);
                return sb.toString();
            }
        }
        int len = accountNumber.length();
        return accountNumber.substring(0, 3) + "***" + accountNumber.substring(len - 3);
    }

    /**
     * 1차 정산 승인
     */
    @Transactional
    public Settlement approveFirstSettlement(Long settlementId, String comment) {
        Settlement settlement = settlementRepository.findById(settlementId)
                 .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));
  
        if (settlement.getStatus() != SettlementStatus.REQUESTED) {
            throw new IllegalStateException("정산 요청(REQUESTED) 상태에서만 1차 승인이 가능합니다.");
        }
  
        settlement.setStatus(SettlementStatus.APPROVED_1ST);
        settlement.setApprovedAt(LocalDateTime.now());
        return settlement;
    }

    /**
     * 영업 관리자 정산 승인
     */
    @Transactional
    public Settlement approveSettlement(Long settlementId, String comment) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));

        if (settlement.getStatus() != SettlementStatus.REQUESTED
                && settlement.getStatus() != SettlementStatus.APPROVED_1ST) {
            throw new IllegalStateException("정산 요청 또는 1차 승인 상태에서만 승인할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setApprovedAt(now);
        if (settlement.getFinalizedAt() == null) {
            settlement.setFinalizedAt(now);
        }
        return settlement;
    }

    @Transactional
    public Settlement rejectSettlement(Long settlementId, String comment) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));

        if (settlement.getStatus() != SettlementStatus.REQUESTED
                && settlement.getStatus() != SettlementStatus.APPROVED_1ST) {
            throw new IllegalStateException("정산 요청 또는 1차 승인 상태에서만 반려가 가능합니다.");
        }

        settlement.setStatus(SettlementStatus.REJECTED);

        // 반려된 정산 건에 속해있던 결제 건들의 settlementId를 null로 돌려주어 재신청 가능하게 함
        List<Payment> payments = paymentRepository.findBySettlementId(settlementId);
        for (Payment p : payments) {
            p.setSettlementId(null);
        }
        paymentRepository.saveAll(payments);

        return settlement;
    }

    /**
     * 최종 정산 확정
     */
    @Transactional
    public Settlement finalizeSettlement(Long settlementId, String comment) {
        Settlement settlement = settlementRepository.findById(settlementId)
                 .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));
  
        if (settlement.getStatus() != SettlementStatus.APPROVED_1ST) {
            throw new IllegalStateException("1차 승인(APPROVED_1ST) 상태에서만 최종 확정이 가능합니다.");
        }
  
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setFinalizedAt(LocalDateTime.now());
        return settlement;
    }

    /**
     * 특정 정산 건의 상세 내역을 조회합니다. (본사 관리자용)
     */
    @Transactional(readOnly = true)
    public List<PaymentRepository.SettlementDetailProjection> getSettlementDetails(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));

        return paymentRepository.findSettlementDetails(settlementId);
    }
}
