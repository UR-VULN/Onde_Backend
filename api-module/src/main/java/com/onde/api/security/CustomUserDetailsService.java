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
        return memberRepository.findByAuthSubjectId(identifier)
                .map(CustomUserDetails::new)
                .or(() -> memberRepository.findByEmail(identifier)
                        .map(CustomUserDetails::new))
                .or(() -> memberRepository.findByProviderId(identifier)
                        .map(CustomUserDetails::new))
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
