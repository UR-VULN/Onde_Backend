package com.onde.api.application.admin;

import com.onde.api.application.admin.dto.MemberAdminResponse;
import com.onde.api.application.admin.dto.MemberSearchRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;

    // 1. 회원 다중 필터 조회
    @Transactional(readOnly = true)
    public Page<MemberAdminResponse> getMembers(MemberSearchRequest request, Pageable pageable) {
        Page<Member> members = memberRepository.searchMembersAdmin(
                request.getRole(),
                request.getKeyword(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
        // Entity Page를 DTO Page로 변환
        return members.map(MemberAdminResponse::from);
    }

    // 2. 블랙리스트 지정 및 강제 로그아웃 (Redis RT 삭제)
    @Transactional
    public void blacklistMember(Long memberId) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. DB 상태 업데이트 (Dirty Checking)
       member.updateStatus(MemberStatus.BANNED); 

        // 3. Redis에서 Refresh Token 강제 삭제 -> 다음 토큰 재발급 시 401 발생 (강제 로그아웃 효과)
        String redisKey = "RT:" + member.getEmail();
        redisTemplate.delete(redisKey);
    }

    @Transactional
    public void updateMemberRole(Long targetMemberId, Long currentAdminId, MemberRole newRole) {
        // 1. 자기 자신 권한 변경 방지 로직
        if (targetMemberId.equals(currentAdminId)) {
            throw new IllegalArgumentException("본인의 권한은 변경할 수 없습니다.");
        }
        
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
                
        member.updateRole(newRole);
    }
}