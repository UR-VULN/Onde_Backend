package com.onde.api.application.admin;

import com.onde.api.application.admin.dto.MemberAdminResponse;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<MemberAdminResponse> getMembers(String role, String status, String name, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Member> members = memberRepository.searchMembersAdmin(
                role,
                status,
                name,
                startDate,
                endDate,
                pageable
        );
        return members.map(MemberAdminResponse::from);
    }
}
