package com.onde.admin.application.member;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.onde.admin.application.member.dto.MemberAdminResponse;
import com.onde.admin.application.member.dto.MemberSearchRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.repository.FcmTokenRepository;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;
    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    // 1. 회원 다중 필터 조회
    @Transactional(readOnly = true)
    public Page<MemberAdminResponse> getMembers(MemberSearchRequest request, Pageable pageable) {
        String name = request.getName() != null ? request.getName() : request.getKeyword();
        Page<Member> members = memberRepository.searchMembersAdmin(
                request.getRole(),
                request.getStatus(),
                name,
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
        // Entity Page를 DTO Page로 변환
        return members.map(MemberAdminResponse::from);
    }

    // 2. 블랙리스트 지정 및 강제 로그아웃 (Redis RT 삭제)
    @Transactional
    public void blacklistMember(Long memberId, String reason) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // [중복 차단 방지] 이미 블랙리스트 상태인 경우 ConflictException 반환
        if (member.getRole() == com.onde.core.entity.member.MemberRole.BLACKLIST || 
            member.getStatus() == com.onde.core.entity.member.MemberStatus.BANNED) {
            throw new com.onde.core.exception.ConflictException(com.onde.core.exception.ErrorCode.MEMBER_ALREADY_BANNED);
        }

        // 2. 명세 기준으로 role을 BLACKLIST로 변경하고 서비스 상태도 정지 처리합니다.
        member.updateRole(MemberRole.BLACKLIST);
        member.updateStatus(MemberStatus.BANNED);

        // 3. Redis에서 Refresh Token 강제 삭제 -> 다음 토큰 재발급 시 401 발생 (강제 로그아웃 효과)
        String redisKey = "RT:" + member.getEmail();
        redisTemplate.delete(redisKey);

        sendSinglePush(memberId, "블랙리스트 처리 안내",
                "계정이 블랙리스트 처리되어 모든 기기에서 로그아웃됩니다." +
                        (reason != null && !reason.isBlank() ? " 사유: " + reason : ""));
    }

    @Transactional
    public MemberRole updateMemberRole(Long targetMemberId, Long currentAdminId, MemberRole newRole) {
        // 1. 자기 자신 권한 변경 방지 로직
        if (targetMemberId.equals(currentAdminId)) {
            throw new IllegalArgumentException("본인의 권한은 변경할 수 없습니다.");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("변경할 권한이 필요합니다.");
        }

        // 2. 비즈니스 규칙: 역할 전환 API로는 오직 일반 회원(USER)과 판매자(SELLER)로의 변경만 허용함
        if (newRole != MemberRole.USER && newRole != MemberRole.SELLER) {
            throw new IllegalArgumentException("허용되지 않는 권한 변경 요청입니다. (USER 또는 SELLER로만 전환 가능합니다)");
        }
        
        Member caller = memberRepository.findById(currentAdminId)
                .orElseThrow(() -> new IllegalArgumentException("요청한 관리자를 찾을 수 없습니다."));
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("변경 대상 회원을 찾을 수 없습니다."));

        MemberRole callerRole = caller.getRole();
        MemberRole targetOldRole = target.getRole();

        // 3. 권한 등급 상한선 및 위임 권한 검증
        int callerLevel = getRoleLevel(callerRole);
        int targetOldLevel = getRoleLevel(targetOldRole);
        int newRoleLevel = getRoleLevel(newRole);

        // 자신의 권한보다 높은 등급의 권한을 부여할 수 없음
        if (newRoleLevel < callerLevel) {
            throw new IllegalArgumentException("자신의 권한보다 높은 등급의 권한을 부여할 수 없습니다.");
        }

        // 자신보다 높은 등급을 가진 관리자의 권한을 변경할 수 없음
        if (targetOldLevel < callerLevel) {
            throw new IllegalArgumentException("자신보다 높은 등급을 가진 관리자의 권한을 변경할 수 없습니다.");
        }

        // SUPER_ADMIN 권한 부여 및 박탈은 최고 관리자(SUPER_ADMIN)만 수행 가능
        if (newRole == MemberRole.SUPER_ADMIN || targetOldRole == MemberRole.SUPER_ADMIN) {
            if (callerRole != MemberRole.SUPER_ADMIN) {
                throw new IllegalArgumentException("최고 관리자(SUPER_ADMIN) 권한 부여 및 박탈은 최고 관리자만 가능합니다.");
            }
        }
                
        target.updateRole(newRole);
        return newRole;
    }

    private int getRoleLevel(MemberRole role) {
        if (role == MemberRole.SUPER_ADMIN) return 1;
        if (role == MemberRole.SELLER_ADMIN) return 2;
        if (role == MemberRole.USER_ADMIN) return 3;
        return 4;
    }

    @Transactional
    public MemberStatus updateMemberStatus(Long memberId, MemberStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 회원 상태가 필요합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.updateStatus(newStatus);
        return newStatus;
    }

    private void sendSinglePush(Long memberId, String title, String body) {
        List<FcmToken> tokens = fcmTokenRepository.findByMemberId(memberId);
        tokens.forEach(token -> {
            Message message = Message.builder()
                    .setToken(token.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            try {
                firebaseMessaging.send(message);
                log.info("Blacklist FCM push sent successfully to memberId={}", memberId);
            } catch (Exception e) {
                log.warn("Blacklist FCM push failed for token={}: {}", token.getFcmToken(), e.getMessage());
            }
        });
    }
}
