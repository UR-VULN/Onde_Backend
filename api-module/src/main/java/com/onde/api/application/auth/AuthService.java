package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.SignupRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String signup(SignupRequest request) {
        // 이메일 중복 검증
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화 및 Member 엔티티 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);

        return "회원가입이 성공적으로 완료되었습니다.";
    }
}