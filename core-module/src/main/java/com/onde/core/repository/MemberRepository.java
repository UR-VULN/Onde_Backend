package com.onde.core.repository;

import com.onde.core.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // 이메일로 회원 정보 단건 조회 (로그인 시 사용)
    Optional<Member> findByEmail(String email);
    
    // 이메일 중복 가입 방지용 확인
    boolean existsByEmail(String email);
}