package com.onde.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 및 CORS 설정을 담당하는 보안 설정 클래스입니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * HTTP 보안 필터 체인을 구성합니다.
     * REST API 서버에 최적화하여 CSRF, 로그인 폼, HTTP Basic 인증을 비활성화하고 세션을 Stateless 모드로 작동하게 설정합니다.
     *
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 예외 상황 시 던져짐
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 커스텀 CORS 설정을 등록합니다.
            .csrf(AbstractHttpConfigurer::disable) // REST API 서버이므로 CSRF 보호 기능을 비활성화합니다.
            .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 페이지 렌더링 및 폼 로그인 처리를 비활성화합니다.
            .httpBasic(AbstractHttpConfigurer::disable) // 기본 HTTP 기본 인증 창을 비활성화합니다.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 서버측에 저장하지 않고 JWT 토큰 방식으로 사용하기 위해 무상태(Stateless)로 설정합니다.
            .authorizeHttpRequests(auth -> auth
                // 현재 기능 개발 및 로컬 테스트 단계이므로 모든 /api/** 및 기타 요청 경로를 임시로 무인증(permitAll) 개방해 두었습니다.
                .requestMatchers("/api/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * 프론트엔드 연동을 위한 CORS(Cross-Origin Resource Sharing) 설정을 구성합니다.
     * React, Vite 등 개발 단계에서 서로 다른 포트(5173, 3000)를 사용하는 오리진(Origin) 간 통신을 가능하게 합니다.
     *
     * @return CORS 설정 소스 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프론트엔드 로컬 서버 오리진 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        
        // 허용할 HTTP 메서드 목록 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 브라우저가 보낼 수 있는 모든 요청 헤더(Headers) 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 쿠키 및 HTTP 인증 자격 증명(Authorization 헤더 등)을 요청 시 주고받을 수 있도록 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로(/**)에 대해 위의 CORS 설정을 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

