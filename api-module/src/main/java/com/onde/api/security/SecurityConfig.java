package com.onde.api.security;

import com.onde.api.security.oauth2.CustomOAuth2UserService;
import com.onde.api.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 빈으로 관리되는 컴포넌트들을 주입받음
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // 소셜 로그인 관련 컴포넌트
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Value("${management.health.allowed-ip:127.0.0.1}")
    private String allowedIp;

    /**
     * [1순위 필터 체인] 인프라 헬스 체크 전용 서브 시스템 (외부 IP 차단)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain healthSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/health/**")
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health/**").access((authentication, context) -> 
                    new AuthorizationDecision(new IpAddressMatcher(allowedIp).matches(context.getRequest())))
                .anyRequest().denyAll()
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 우리의 커스텀 CORS 설정을 등록하여 프론트 통신 차단 해제 (이식 완료)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. [팀원 스펙] 401, 403 예외 처리 핸들러 등록 유지
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // 3. URL 경로별 접근 권한 세팅 (두 코드의 허용 경로 대통합)
                .authorizeHttpRequests(auth -> auth
                        // ALL (누구나 접근 가능한 공개 경로)
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/report/integrated", "/api/v1/test/**").permitAll()
                        .requestMatchers("/api/v1/flights/search").permitAll()
                        .requestMatchers("/api/v1/insurance/calculate", "/api/v1/insurances/calculate").permitAll()
                        .requestMatchers("/api/v1/inventory/**", "/api/inventory/**").permitAll()
                        .requestMatchers("/api/v1/accommodations/**", "/api/v1/cars/**", "/api/v1/rental_cars/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/properties", "/api/v1/property").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*/comments").permitAll()

                        // SELLER (판매자만 접근 가능)
                        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")



                        // [보안 강화] 그 외의 모든 예약, 정산 등 핵심 요청은 무조건 로그인(인증)된 사용자만 접근 허용
                        // 원래 우리 코드의 .anyRequest().permitAll()은 보안 멍청이 코드가 될 위험이 커서 팀원의
                        // .authenticated()로 잠갔습니다.
                        .anyRequest().authenticated())

                // 4. [팀원 스펙] OAuth2 소셜 로그인 파이프라인 조립 완벽 유지
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 5. 스프링 빈 컨테이너가 안전하게 관리하는 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                // (new 키워드로 수동 생성하면 의존성 주입이 다 깨지므로 팀원 방식이 100% 맞습니다)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 프론트엔드(React, Vite 등) 연동을 위한 CORS 설정 구성 (우리 코드 이식 완료)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://onde.click",
                "https://www.onde.click",
                "https://admin.onde.click"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
