package com.onde.api.application.payment;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.payment.UserWallet;
import com.onde.core.entity.payment.WalletTransaction;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.UserWalletRepository;
import com.onde.core.repository.WalletTransactionRepository;
import com.onde.core.exception.ValidationException;
import com.onde.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    /** 1회 지갑 충전 허용 최대 금액 (원) */
    private static final BigDecimal MAX_CHARGE_AMOUNT = new BigDecimal("10000000");

    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return userWalletRepository.findByMemberId(userId)
                .map(UserWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public BigDecimal charge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (amount.compareTo(MAX_CHARGE_AMOUNT) > 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.addBalance(amount);

        recordTransaction(wallet, amount, "CHARGE", null, "지갑 충전");
        return wallet.getBalance();
    }

    @Transactional
    public void deduct(Long userId, BigDecimal amount, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.subtractBalance(amount);

        recordTransaction(wallet, amount.negate(), "PAYMENT", referenceId, "결제 차감");
    }

    @Transactional
    public void refund(Long userId, BigDecimal amount, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.addBalance(amount);

        recordTransaction(wallet, amount, "REFUND", referenceId, "결제 취소 환불");
    }

    private UserWallet getOrCreateWallet(Long userId) {
        return userWalletRepository.findByMemberId(userId).orElseGet(() -> {
            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            UserWallet newWallet = UserWallet.builder()
                    .member(member)
                    .balance(BigDecimal.ZERO)
                    .build();
            return userWalletRepository.save(newWallet);
        });
    }

    private void recordTransaction(UserWallet wallet, BigDecimal amount, String type, String referenceId, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .referenceId(referenceId)
                .description(description)
                .build();
        walletTransactionRepository.save(transaction);
    }
}
