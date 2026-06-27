package com.onde.admin.security;

import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class AdminMemberIdentitySupport {

    private final MemberRepository memberRepository;

    public Member requireMember(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("관리자를 찾을 수 없습니다.");
        }
        return resolveMember(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
    }

    public Long requireMemberId(UserDetails userDetails) {
        return requireMember(userDetails).getId();
    }

    public Long requireMemberId(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("관리자를 찾을 수 없습니다.");
        }
        return resolveMember(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."))
                .getId();
    }

    private java.util.Optional<Member> resolveMember(String identifier) {
        return memberRepository.findByEmail(identifier)
                .or(() -> memberRepository.findByAuthSubjectId(identifier))
                .or(() -> memberRepository.findByProviderId(identifier));
    }
}
