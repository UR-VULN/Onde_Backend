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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final com.onde.core.security.TokenBlacklistService tokenBlacklistService;

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
                log.info("[JwtAuthenticationFilter] Extracted token: {}, Checking blacklist...", token.substring(0, Math.min(token.length(), 15)) + "...");
                // [보안 강화 - WEB-9] 로그아웃된 블랙리스트 토큰 차단
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("[JwtAuthenticationFilter] 로그아웃된 토큰으로 접근을 시도하여 차단합니다. Token: {}", token.substring(0, Math.min(token.length(), 15)) + "...");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                    return;
                }
                log.info("[JwtAuthenticationFilter] Token is not blacklisted. Proceeding to validate...");

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
            // 테스트 헬퍼(우회 로직) 전체 삭제 완료

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
}