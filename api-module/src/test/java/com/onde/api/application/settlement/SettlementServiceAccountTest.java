package com.onde.api.application.settlement;

import com.onde.api.application.settlement.dto.SellerAccountRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.exception.BusinessException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SellerAccountRepository;
import com.onde.core.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceAccountTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SellerAccountRepository sellerAccountRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NtsBusinessVerificationService ntsBusinessVerificationService;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    void registerOrUpdateAccountVerifiesBusinessBeforeSaving() {
        Member seller = seller(41L);
        when(memberRepository.findById(41L)).thenReturn(Optional.of(seller));
        when(sellerAccountRepository.findByMemberId(41L)).thenReturn(Optional.empty());
        when(ntsBusinessVerificationService.verifyBusiness("1234567890", "홍길동", "20200101"))
                .thenReturn(NtsBusinessVerificationService.BusinessVerificationResult.valid("확인되었습니다."));
        when(sellerAccountRepository.save(any(SellerAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerAccount saved = settlementService.registerOrUpdateAccount(41L, request("123-45-67890", "홍길동", "2020-01-01"));

        assertEquals("국민은행", saved.getBankName());
        assertEquals("1234567890", saved.getBusinessNumber());
        assertEquals("홍길동", saved.getRepresentativeName());
        assertEquals("20200101", saved.getOpenedAt());
        assertEquals("123456789012", saved.getAccountNumber());
    }

    @Test
    void registerOrUpdateAccountRejectsInvalidBusinessInformation() {
        Member seller = seller(41L);
        when(memberRepository.findById(41L)).thenReturn(Optional.of(seller));
        when(sellerAccountRepository.findByMemberId(41L)).thenReturn(Optional.empty());
        when(ntsBusinessVerificationService.verifyBusiness("1234567890", "가짜대표", "19990101"))
                .thenReturn(NtsBusinessVerificationService.BusinessVerificationResult.invalid("확인할 수 없습니다."));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.registerOrUpdateAccount(41L, request("1234567890", "가짜대표", "19990101"))
        );

        assertEquals("확인할 수 없습니다.", exception.getMessage());
        verify(sellerAccountRepository, never()).save(any());
    }

    private SellerAccountRequest request(String businessNumber, String representativeName, String openedAt) {
        SellerAccountRequest request = new SellerAccountRequest();
        request.setBankName("국민은행");
        request.setAccountNumber("123-456-789012");
        request.setAccountHolder("온데테스트");
        request.setBusinessNumber(businessNumber);
        request.setRepresentativeName(representativeName);
        request.setOpenedAt(openedAt);
        return request;
    }

    private Member seller(Long id) {
        Member member = Member.builder()
                .email("seller@onde.test")
                .password("password")
                .role(MemberRole.SELLER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
