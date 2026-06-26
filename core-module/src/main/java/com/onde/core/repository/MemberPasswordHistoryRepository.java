package com.onde.core.repository;

import com.onde.core.entity.member.MemberPasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberPasswordHistoryRepository extends JpaRepository<MemberPasswordHistory, Long> {

    void deleteByMemberId(Long memberId);

    Optional<MemberPasswordHistory> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}
