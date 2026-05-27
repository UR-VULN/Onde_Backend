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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 쿠키에서 accessToken 추출
        String token = resolveTokenFromCookie(request, "accessToken");

        // 2. 토큰이 존재하고 유효하다면 인증 정보 세팅
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmail(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            // SecurityContext에 인증 객체 저장 -> 이후 컨트롤러에서 로그인된 유저로 인식함
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. 다음 필터로 이동
        filterChain.doFilter(request, response);
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