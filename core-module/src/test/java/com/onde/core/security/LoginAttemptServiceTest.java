package com.onde.core.security;



import com.onde.core.config.LoginLockProperties;

import com.onde.core.entity.member.Member;

import com.onde.core.entity.member.MemberRole;

import com.onde.core.exception.BusinessException;

import com.onde.core.exception.ErrorCode;

import com.onde.core.repository.MemberRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.lang.NonNull;

import org.springframework.test.util.ReflectionTestUtils;



import java.time.LocalDateTime;

import java.util.Objects;

import java.util.Optional;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.argThat;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



@SuppressWarnings("null")

@ExtendWith(MockitoExtension.class)

class LoginAttemptServiceTest {



    private static final Long MEMBER_ID = 1L;



    @Mock

    private MemberRepository memberRepository;



    @Mock

    private LoginLockProperties loginLockProperties;



    @InjectMocks

    private LoginAttemptService loginAttemptService;



    private Member member;



    @BeforeEach

    void setUp() {

        when(loginLockProperties.getMaxFailedAttempts()).thenReturn(5);

        when(loginLockProperties.getLockMinutes()).thenReturn(30);

        member = createMember();

        ReflectionTestUtils.setField(member, "id", MEMBER_ID);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

    }



    @Test

    void locksAccountAfterMaxFailures() {

        for (int i = 0; i < 4; i++) {

            loginAttemptService.recordFailure(MEMBER_ID);

        }

        assertEquals(4, member.getFailedLoginAttempts());



        loginAttemptService.recordFailure(MEMBER_ID);



        assertTrue(member.isLoginLocked());

        assertEquals(0, member.getFailedLoginAttempts());

        verify(memberRepository).save(argThat(saved -> saved.isLoginLocked()));

    }



    @Test

    void rejectsLockedAccount() {

        member.lockLoginUntil(LocalDateTime.now().plusMinutes(10));



        BusinessException exception = assertThrows(

                BusinessException.class,

                () -> loginAttemptService.assertNotLocked(member));



        assertEquals(ErrorCode.LOGIN_LOCKED, exception.getErrorCode());

    }



    @Test

    void clearsExpiredLock() {

        member.lockLoginUntil(LocalDateTime.now().minusMinutes(1));

        setFailedAttempts(member, 3);



        loginAttemptService.assertNotLocked(member);



        assertEquals(0, member.getFailedLoginAttempts());

        verify(memberRepository).save(argThat(saved ->

                saved.getFailedLoginAttempts() == 0 && saved.getLoginLockedUntil() == null));

    }



    @NonNull

    private static Member createMember() {

        return Objects.requireNonNull(Member.builder()

                .email("user@onde.com")

                .password("encoded")

                .role(MemberRole.USER)

                .build());

    }



    private static void setFailedAttempts(@NonNull Member target, int attempts) {

        ReflectionTestUtils.setField(target, "failedLoginAttempts", attempts);

    }

}

