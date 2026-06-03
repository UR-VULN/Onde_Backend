package com.onde.core.repository;

import com.onde.core.entity.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {
    Page<Member> searchMembersAdmin(String role, String status, String name, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
}
