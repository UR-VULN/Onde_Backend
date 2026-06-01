package com.onde.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final AdminJwtTokenProvider adminJwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. REST API 환경을 위한 기본 로그인 방어 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 2. JWT 인증을 사용하므로 세션을 생성하지 않도록 설정 (Stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. 인가(Authorization) 규칙 정의
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN") // 어드민 전용 API 권한 제한
                .anyRequest().permitAll() // 그 외의 요청은 허용
            )
            
            // 4. 아까 커스텀하게 통합한 AdminJwtAuthenticationFilter를 시큐리티 필터 흐름 앞에 주입
            .addFilterBefore(new AdminJwtAuthenticationFilter(adminJwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}