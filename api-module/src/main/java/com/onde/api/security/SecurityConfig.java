package com.onde.api.security;

import com.onde.api.security.oauth2.CustomOAuth2UserService;
import com.onde.api.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // 소셜 로그인 관련 컴포넌트 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 401, 403 예외 처리 핸들러 등록
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )

            // URL 경로별 접근 권한 세팅
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()    // 회원가입, 로그인은 누구나
                .requestMatchers("/api/v1/flights/search").permitAll() // 항공권 검색 누구나
                .requestMatchers("/api/v1/accommodations/**", "/api/v1/cars/**").permitAll() // 숙소/렌터카 검색 누구나
                .requestMatchers("/api/v1/seller/**").hasRole("SELLER") // 판매자 페이지는 SELLER만
                .requestMatchers("/api/v1/admin/**").hasAnyRole("GENERAL_ADMIN", "SALES_ADMIN", "SUPER_ADMIN") // 관리자 페이지
                .anyRequest().authenticated() // 그 외의 모든 요청은 로그인(인증)된 사용자만 접근 가능
            )

            // OAuth2 소셜 로그인 파이프라인 조립
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )

            // 커스텀 JWT 필터를 기본 아이디/비밀번호 필터 '앞'에 배치
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}