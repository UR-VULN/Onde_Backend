package com.onde.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final AdminAuthenticationEntryPoint adminAuthenticationEntryPoint;
    private final AdminAccessDeniedHandler adminAccessDeniedHandler;

    /**
     * [1순위 필터 체인] 인프라 헬스 체크 전용 서브 시스템
     */
    @Bean
    @Order(1)
    public SecurityFilterChain healthSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/admin/health/**")
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/admin/health/**").access((authentication, context) -> 
                    new AuthorizationDecision(new IpAddressMatcher(allowedIp).matches(context.getRequest())))
                .anyRequest().denyAll()
            );

        return http.build();
    }

    /**
     * [2순위 필터 체인] 일반 어드민 비즈니스 로직 시스템
     */
@Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. REST API 환경을 위한 기본 로그인 방어 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 2. JWT 인증을 사용하므로 세션을 생성하지 않도록 설정 (Stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(adminAuthenticationEntryPoint)
                .accessDeniedHandler(adminAccessDeniedHandler))
            
            // 3. 인가(Authorization) 규칙 정의
            .authorizeHttpRequests(auth -> auth
                // 어드민 전용 API 권한 제한
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "SELLER_ADMIN", "USER_ADMIN") 
                
                // 그 외의 요청은 허용
                .anyRequest().permitAll() 
            )
            
            // 4. 커스텀하게 통합한 AdminJwtAuthenticationFilter를 시큐리티 필터 흐름 앞에 주입
            .addFilterBefore(new AdminJwtAuthenticationFilter(adminJwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
