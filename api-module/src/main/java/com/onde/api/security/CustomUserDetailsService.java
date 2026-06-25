package com.onde.api.security;

import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. 이메일로 먼저 조회
        return memberRepository.findByEmail(identifier)
                .map(CustomUserDetails::new)
                .or(() -> 
                    // 2. 이메일로 없으면 providerId로 조회
                    memberRepository.findByProviderId(identifier)
                        .map(CustomUserDetails::new)
                )
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + identifier));
    }

    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        return memberRepository.findById(id)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + id));
    }
}