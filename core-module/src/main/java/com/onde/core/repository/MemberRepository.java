package com.onde.core.repository;

import com.onde.core.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    
    // 이메일로 회원 정보 단건 조회 (로그인 시 사용)
    Optional<Member> findByEmail(String email);
    
    // 이메일 중복 가입 방지용 확인
    boolean existsByEmail(String email);

    // 소셜 로그인 (provider, providerId 기준) 회원 조회
    Optional<Member> findByProviderAndProviderId(com.onde.core.entity.member.AuthProvider provider, String providerId);

    // 식별자(providerId)로 회원 조회
    Optional<Member> findByProviderId(String providerId);
}