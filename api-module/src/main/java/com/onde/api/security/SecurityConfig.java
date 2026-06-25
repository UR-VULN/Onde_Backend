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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
                // 1. 우리의 커스텀 CORS 설정을 등록하여 프론트 통신 차단 해제 (이식 완료)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> {
                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    requestHandler.setCsrfRequestAttributeName(null);
                    csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/v1/auth/**");
                })
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
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/report/integrated", "/api/v1/test/**").permitAll()
                        .requestMatchers("/api/v1/flights/search").permitAll()
                        .requestMatchers("/api/v1/insurance/calculate", "/api/v1/insurances/calculate").permitAll()
                        .requestMatchers("/api/v1/inventory/**", "/api/inventory/**").permitAll()
                        .requestMatchers("/api/v1/accommodations/**", "/api/v1/cars/**", "/api/v1/rental_cars/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/properties", "/api/v1/property").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*/comments").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/seller/account/test-bucket").permitAll()

                        // SELLER (판매자만 접근 가능)
                        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")


                        // [보안 강화] 명시적인 HTTP 메소드 화이트리스트 처리
                        // OPTIONS는 CORS Preflight를 위해 허용, 나머지는 기본적으로 인증 요구
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/**").authenticated()
                        
                        // 명시되지 않은 모든 요청(TRACE, CONNECT 등)은 즉시 차단
                        .anyRequest().denyAll())

                // 4. [팀원 스펙] OAuth2 소셜 로그인 파이프라인 조립 완벽 유지
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 5. 스프링 빈 컨테이너가 안전하게 관리하는 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                // (new 키워드로 수동 생성하면 의존성 주입이 다 깨지므로 팀원 방식이 100% 맞습니다)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 6. [XSS 방지] Content-Security-Policy 헤더 추가
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; object-src 'none'; frame-ancestors 'none'")
                        )
                );

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
        // [이행진단 1] 와일드카드(*) 대신 명시적 Allowed Headers 적용
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "Accept", 
                "Origin", 
                "X-Requested-With", 
                "X-XSRF-TOKEN", 
                "X-CSRF-TOKEN",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
