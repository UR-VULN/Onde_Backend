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
            // 1. 헤더에서 토큰 추출
            String token = resolveTokenFromHeader(request);

            // 2. 만약 헤더에 없다면 쿠키에서 토큰 추출
            if (token == null) {
                token = resolveTokenFromCookie(request, "accessToken");
            }

            // 3. 토큰이 존재하고 유효하다면 인증 정보 세팅
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmail(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                // SecurityContext에 인증 객체 저장 -> 이후 컨트롤러에서 로그인된 유저로 인식함
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 401 에러의 진짜 원인을 터미널에 출력해 주는 디버깅 코드
            System.out.println("❌ JWT 필터 통과 실패 (인증 에러): " + e.getMessage());
            e.printStackTrace();
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