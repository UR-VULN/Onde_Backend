package com.onde.api.application.member;

import com.onde.core.security.SensitiveRevealAuthService;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveRevealAuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SensitiveRevealAuthService sensitiveRevealAuthService;

    @Test
    void requirePasswordVerifiedMember_returnsMemberWhenPasswordMatches() {
        Member member = Member.builder().id(1L).password("encoded").build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("correct", "encoded")).thenReturn(true);

        Member result = sensitiveRevealAuthService.requirePasswordVerifiedMember(1L, "correct");

        assertEquals(1L, result.getId());
    }

    @Test
    void requirePasswordVerifiedMember_throwsWhenPasswordMismatch() {
        Member member = Member.builder().id(1L).password("encoded").build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sensitiveRevealAuthService.requirePasswordVerifiedMember(1L, "wrong"));

        assertEquals(ErrorCode.SENSITIVE_REVEAL_PASSWORD_MISMATCH, ex.getErrorCode());
    }
}
