package com.onde.admin.security;

import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return memberRepository.findByAuthSubjectId(identifier)
                .map(AdminUserDetails::new)
                .or(() -> memberRepository.findByEmail(identifier)
                        .map(AdminUserDetails::new))
                .or(() -> memberRepository.findByProviderId(identifier)
                        .map(AdminUserDetails::new))
                .orElseThrow(() -> new UsernameNotFoundException("관리자를 찾을 수 없습니다."));
    }
}
