package com.onde.api.security;

import com.onde.core.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
try {
            System.out.println("\n===== [JWT 필터 디버깅 시작] =====");
            
            // 1. 헤더에서 토큰 추출
            String token = resolveTokenFromHeader(request);
            System.out.println("▶️ 1. 헤더 토큰: " + (token != null ? "발견됨 (길이: " + token.length() + ")" : "없음"));

            // 2. 쿠키에서 토큰 추출 (헤더에 없을 경우)
            if (token == null) {
                token = resolveTokenFromCookie(request, "accessToken");
                System.out.println("▶️ 2. 쿠키 토큰: " + (token != null ? "발견됨" : "없음"));
            }

            // 3. 토큰 검증 및 인증 처리
            if (token != null) {
                boolean isValid = jwtTokenProvider.validateToken(token);
                System.out.println("▶️ 3. 토큰 유효성 검사 결과: " + (isValid ? "✅ 통과" : "❌ 실패 (만료 또는 훼손)"));
                
                if (isValid) {
                    String email = jwtTokenProvider.getEmail(token);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("▶️ 4. 인증 완료! 부여된 권한: " + userDetails.getAuthorities());
                }
            } else {
                // [테스트 헬퍼] 토큰이 없지만 어드민 테스트용 HTTP 헤더 정보가 넘어온 경우 모의 인증 처리
                String adminId = request.getHeader("X-Admin-Id");
                String adminRole = request.getHeader("X-Admin-Role");

                if (adminRole != null && !adminRole.isBlank()) {
                    if (adminId == null || adminId.isBlank()) {
                        adminId = "AD-999";
                    }
                    String roleName = adminRole.toUpperCase().startsWith("ROLE_") ? adminRole.toUpperCase() : "ROLE_" + adminRole.toUpperCase();
                    java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.List.of(
                            new SimpleGrantedAuthority(roleName),
                            new SimpleGrantedAuthority("ROLE_ADMIN")
                    );
                    
                    UserDetails principal = new org.springframework.security.core.userdetails.User(adminId, "", authorities);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("▶️ [어드민 테스트 헬퍼] 인증 완료! 부여된 권한: " + authorities);
                } else {
                    System.out.println("▶️ 🚨 요청에 토큰이 아예 존재하지 않습니다!");
                }
            }
            System.out.println("==================================\n");
            
        } catch (Exception e) {
            System.out.println("❌ JWT 필터 통과 중 에러 발생: " + e.getMessage());
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰을 꺼내오는 메서드
    private String resolveTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 진짜 토큰만 잘라서 반환
        }
        return null;
    }

    // 쿠키 배열을 뒤져서 원하는 이름의 쿠키 값을 꺼내오는 유틸리티 메서드
    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null; 
    }
}