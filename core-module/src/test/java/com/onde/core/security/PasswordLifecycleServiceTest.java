package com.onde.core.security;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberPasswordHistoryRepository;
import com.onde.core.validation.PasswordPolicyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordLifecycleServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberPasswordHistoryRepository passwordHistoryRepository;

    @InjectMocks
    private PasswordLifecycleService passwordLifecycleService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = createMemberWithPasswordUpdatedAt(LocalDateTime.now().minusDays(61));
    }

    @Test
    void detectsExpiredUserPassword() {
        assertTrue(passwordLifecycleService.isPasswordExpired(member));
    }

    @Test
    void rejectsReusedPassword() {
        when(passwordEncoder.matches("Abcd1234!", "encoded-old")).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                passwordLifecycleService.changePassword(member, "Abcd1234!", PasswordPolicyLevel.USER));
    }

    @Test
    @SuppressWarnings("null")
    void storesPreviousPasswordBeforeChange() {
        when(passwordEncoder.matches("Abcd1234!", "encoded-old")).thenReturn(false);
        when(passwordHistoryRepository.findTopByMemberIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-new");

        passwordLifecycleService.changePassword(member, "Abcd1234!", PasswordPolicyLevel.USER);

        verify(passwordHistoryRepository).deleteByMemberId(1L);
        verify(passwordHistoryRepository).save(argThat(history ->
                history != null && "encoded-old".equals(history.getPasswordHash())));
        assertFalse(passwordLifecycleService.isPasswordExpired(member));
    }

    @NonNull
    private static Member createMemberWithPasswordUpdatedAt(LocalDateTime passwordUpdatedAt) {
        Member built = Objects.requireNonNull(Member.builder()
                .email("user@onde.com")
                .password("encoded-old")
                .role(MemberRole.USER)
                .build());
        built.markPasswordUpdatedNow();
        ReflectionTestUtils.setField(built, "passwordUpdatedAt", passwordUpdatedAt);
        ReflectionTestUtils.setField(built, "id", 1L);
        return built;
    }
}
