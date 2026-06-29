package com.onde.api.security.oauth2;

import com.onde.api.security.CustomUserDetails;
import com.onde.core.entity.member.AuthProvider;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 구글, 카카오 등 프로바이더 구분 식별값 (google, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = null;
        String name = "";
        String providerId = "";
        AuthProvider provider = AuthProvider.LOCAL;

        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = String.valueOf(attributes.get("sub")); // Google uses "sub"
            provider = AuthProvider.GOOGLE;
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            
            // 카카오에서 email 제공 동의를 받지 않으면 null일 수 있음
            if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                email = (String) kakaoAccount.get("email");
            }
            if (properties != null && properties.containsKey("nickname")) {
                name = (String) properties.get("nickname");
            }
            providerId = String.valueOf(attributes.get("id"));
            provider = AuthProvider.KAKAO;
        }

        // 대조 및 자동 가입 처리 (provider와 providerId 기준 조회)
        AuthProvider finalProvider = provider;
        String finalName = name;
        String finalEmail = email;
        String finalProviderId = providerId;
        
        Member member = memberRepository.findByProviderAndProviderId(finalProvider, finalProviderId)
                .orElseGet(() -> {
                    // 소셜 유저는 더미 패스워드를 인코딩하여 테이블 제약 조건 우회
                    String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
                    
                    // 이메일이 없으면 GUEST, 있으면 USER로 분기 처리
                    MemberRole role = (finalEmail != null && !finalEmail.trim().isEmpty()) ? MemberRole.USER : MemberRole.GUEST;
                    
                    Member newMember = Member.builder()
                            .email(finalEmail)
                            .providerId(finalProviderId)
                            .password(dummyPassword)
                            .name(finalName)
                            .role(role)
                            .status(MemberStatus.ACTIVE)
                            .provider(finalProvider)
                            .build();
                    return memberRepository.save(newMember);
                });

        return new CustomUserDetails(member, attributes);
    }
}