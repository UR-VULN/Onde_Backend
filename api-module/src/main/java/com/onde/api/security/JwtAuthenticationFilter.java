package com.onde.api.security;

import com.onde.core.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 헤더에서 토큰 추출
            String token = resolveTokenFromHeader(request);

            // 2. 쿠키에서 토큰 추출 (헤더에 없을 경우)
            if (token == null) {
                token = resolveTokenFromCookie(request, "accessToken");
            }

            // 3. 토큰 검증 및 인증 처리
            if (token != null) {
                // alg=none 조작 및 서명 없는 토큰 사전 차단
                if (isVulnerableToken(token)) {
                    log.error("[Security] 비정상적인 JWT 구조 또는 alg=none 공격 탐지됨");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "변조된 토큰입니다.");
                    return; // 더 이상 진행하지 않고 즉시 차단
                }

                boolean isValid = jwtTokenProvider.validateToken(token);

                if (isValid) {
                    String identifier = jwtTokenProvider.getSubject(token);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("[JwtAuthenticationFilter] 유효하지 않거나 만료된 토큰입니다.");
                }
            } 

        } catch (Exception e) {
            log.warn("[JwtAuthenticationFilter] JWT 필터 처리 중 에러 발생: {}", e.getMessage());
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    private String resolveTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

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

    // JWT Header를 디코딩하여 alg=none 여부 및 3단 구조(Header.Payload.Signature)를 강제 검증
    private boolean isVulnerableToken(String token) {
        try {
            String[] parts = token.split("\\.");
            
            // JWT는 무조건 점(.)을 기준으로 3조각이어야 합니다. 서명을 지운 alg=none 공격 차단
            if (parts.length != 3) {
                return true;
            }
            
            // Header 검사 (Base64 URL 디코딩)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            String normalizedHeader = headerJson.replaceAll("\\s+", "").toLowerCase();
            
            // Header에 alg: none 이라고 명시되어 있다면 차단
            if (normalizedHeader.contains("\"alg\":\"none\"")) {
                return true;
            }
        } catch (Exception e) {
            // 디코딩 조차 안 되는 이상한 토큰도 모두 방어
            return true; 
        }
        return false;
    }
}