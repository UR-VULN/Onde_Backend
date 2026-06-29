package com.onde.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final AdminAuthenticationEntryPoint adminAuthenticationEntryPoint;
    private final AdminAccessDeniedHandler adminAccessDeniedHandler;
    private final com.onde.core.security.TokenBlacklistService tokenBlacklistService;

    @Value("${management.health.allowed-ip}")
    private String allowedIp;

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
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // [보안 지침 보완] JWT Stateless 아키텍처이므로 서블릿 세션 고정 방지는 불필요하여 배제합니다.
            )
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
            // [보안 강화 - WEB-13] 클릭재킹, XSS 방어 및 HTTPS 강제를 위한 전역 보안 헤더 HSTS, CSP, X-Frame-Options 추가
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'"))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            )

            // 1. REST API 환경을 위한 기본 로그인 방어 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 2. JWT 인증을 사용하므로 세션을 생성하지 않도록 설정 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // [보안 지침 보완 - ADMIN-9] JWT Stateless 아키텍처이므로 서블릿 세션 고정 방지는 실제로 작동하지 않아 제거했습니다.
                // 세션 고정 공격 및 토큰 재사용 대응은 TokenBlacklistService(Redis 블랙리스트 필터)와 Refresh Token 회전으로 철저히 제어됩니다.
            )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(adminAuthenticationEntryPoint)
                .accessDeniedHandler(adminAccessDeniedHandler))
            
            // 3. 인가(Authorization) 규칙 정의
            .authorizeHttpRequests(auth -> auth
                // [보안 강화 - WEB-15] 불필요하거나 취약한 HTTP TRACE 메서드는 차단하고 OPTIONS는 Preflight 용도로만 명시적 허용
                .requestMatchers(org.springframework.http.HttpMethod.TRACE, "/**").denyAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                // 어드민 전용 API 권한 제한
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "SELLER_ADMIN", "USER_ADMIN") 
                
                // 그 외의 요청은 허용
                .anyRequest().permitAll() 
            )
            
            // 4. 커스텀하게 통합한 AdminJwtAuthenticationFilter를 시큐리티 필터 흐름 앞에 주입
            .addFilterBefore(new AdminJwtAuthenticationFilter(adminJwtTokenProvider, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
