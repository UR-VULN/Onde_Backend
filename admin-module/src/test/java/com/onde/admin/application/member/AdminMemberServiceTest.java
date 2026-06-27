package com.onde.admin.application.member;

import com.google.firebase.messaging.FirebaseMessaging;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.repository.FcmTokenRepository;
import com.onde.core.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.onde.core.security.AuthSessionRevocationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthSessionRevocationService authSessionRevocationService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    void updateMemberStatusApprovesPendingSeller() {
        Member seller = Member.builder()
                .id(10L)
                .email("seller@onde.com")
                .password("encoded-password")
                .role(MemberRole.SELLER)
                .status(MemberStatus.PENDING)
                .build();
        when(memberRepository.findById(10L)).thenReturn(Optional.of(seller));

        MemberStatus updatedStatus = adminMemberService.updateMemberStatus(10L, MemberStatus.ACTIVE);

        assertEquals(MemberStatus.ACTIVE, updatedStatus);
        assertEquals(MemberStatus.ACTIVE, seller.getStatus());
    }
}
