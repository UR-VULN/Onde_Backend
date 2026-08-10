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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 프론트엔드 도메인에서의 요청을 허용하기 위한 CORS 설정 등록 (허용 목록은 corsConfigurationSource() 참고)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 인증 실패(401)와 권한 부족(403)을 공통 JSON 에러 응답으로 내려주는 핸들러 등록
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // 3. URL 경로별 접근 권한 설정 (인증 없이 열어야 하는 경로만 아래에 명시)
                .authorizeHttpRequests(auth -> auth
                        // ALL (누구나 접근 가능한 공개 경로)
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
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



                        // [보안 강화] 그 외의 모든 예약, 정산 등 핵심 요청은 인증된 사용자만 접근 허용
                        // 기본값을 permitAll()로 두면 신규 엔드포인트를 추가할 때 인증 설정을 누락해도 그대로 공개되므로,
                        // 기본값은 authenticated()로 막고 공개가 필요한 경로만 위에 명시
                        .anyRequest().authenticated())

                // 4. OAuth2 소셜 로그인 처리 (사용자 정보 조회 후 인증 성공 시 토큰 발급 및 리다이렉트)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 5. JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                // 필터를 new로 직접 생성하면 의존성 주입을 받지 못하므로, 스프링 빈으로 주입받아 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 프론트엔드(React, Vite 등) 연동을 위한 CORS 설정 구성.
     * 인증 쿠키를 주고받아야 하므로 allowCredentials(true)를 사용하며,
     * 이 경우 와일드카드 오리진을 쓸 수 없어 허용 도메인을 직접 명시합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://onde.click",
                "https://www.onde.click",
                "https://rookies.onde.click"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
