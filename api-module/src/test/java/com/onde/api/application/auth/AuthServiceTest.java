package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.SignupRequest;
import com.onde.api.application.auth.dto.SignupResponse;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void sellerSignupCreatesPendingMember() {
        SignupRequest request = signupRequest(MemberRole.SELLER);
        when(memberRepository.existsByEmail("seller@onde.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals(MemberRole.SELLER, memberCaptor.getValue().getRole());
        assertEquals(MemberStatus.PENDING, memberCaptor.getValue().getStatus());
        assertEquals(MemberStatus.PENDING, response.getStatus());
    }

    @Test
    void pendingSellerCannotLogin() {
        Member pendingSeller = Member.builder()
                .email("seller@onde.com")
                .password("encoded-password")
                .role(MemberRole.SELLER)
                .status(MemberStatus.PENDING)
                .build();
        when(memberRepository.findByEmail("seller@onde.com")).thenReturn(Optional.of(pendingSeller));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginRequest("seller@onde.com", "password123"))
        );

        assertEquals(ErrorCode.SELLER_PENDING_APPROVAL, exception.getErrorCode());
        verify(jwtTokenProvider, never()).createAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    private SignupRequest signupRequest(MemberRole role) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "seller@onde.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "passwordConfirm", "password123");
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }

    private com.onde.api.application.auth.dto.LoginRequest loginRequest(String email, String password) {
        com.onde.api.application.auth.dto.LoginRequest request = new com.onde.api.application.auth.dto.LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }
}
