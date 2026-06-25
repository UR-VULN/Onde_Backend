package com.onde.admin.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. 헤더에서 Authorization (Bearer JWT) 추출
        String token = resolveToken(request);

        // 2. 헤더에 토큰이 없는 경우 쿠키에서 추출
        if (token == null) {
            token = resolveTokenFromCookie(request, "accessToken");
        }

        // 3. 진짜 JWT 토큰이 넘어왔고 검증이 성공한 경우 인증 처리 (블랙리스트 조회 포함)
        if (token != null) {
            boolean isBlacklisted = Boolean.TRUE.equals(stringRedisTemplate.hasKey("BL:" + token));
            
            if (!isBlacklisted && adminJwtTokenProvider.validateToken(token)) {
                Claims claims = adminJwtTokenProvider.getClaims(token);
                String email = claims.getSubject();
                List<String> rolesList = claims.get("roles", List.class);
                if (rolesList == null) {
                    String singleRole = claims.get("role", String.class);
                    if (singleRole != null) {
                        rolesList = List.of(singleRole);
                    } else {
                        rolesList = List.of();
                    }
                }

                List<SimpleGrantedAuthority> authorities = rolesList.stream()
                        .map(role -> {
                            String r = role.toUpperCase();
                            return new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r);
                        })
                        .collect(Collectors.toList());

                UserDetails principal = new User(email, "", authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, token, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } 


        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}