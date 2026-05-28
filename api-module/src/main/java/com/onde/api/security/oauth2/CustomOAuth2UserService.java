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

        String email = "";
        String name = "";
        AuthProvider provider = AuthProvider.LOCAL;

        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            provider = AuthProvider.GOOGLE;
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            
            email = (String) kakaoAccount.get("email");
            name = (String) properties.get("nickname");
            provider = AuthProvider.KAKAO;
        }

        // 대조 및 자동 가입 처리
        AuthProvider finalProvider = provider;
        String finalName = name;
        
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 소셜 유저는 더미 패스워드를 인코딩하여 테이블 제약 조건 우회
                    String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
                    Member newMember = Member.builder()
                            .email(oAuth2User.getAttribute("email") != null ? oAuth2User.getAttribute("email") : (String)((Map<String, Object>)attributes.get("kakao_account")).get("email"))
                            .password(dummyPassword)
                            .role(MemberRole.USER) // 초기 역할 USER 할당
                            .status(MemberStatus.ACTIVE)
                            .provider(finalProvider) // member 엔티티에 provider 정보 저장
                            .build();
                    return memberRepository.save(newMember);
                });

        return new CustomUserDetails(member, attributes);
    }
}