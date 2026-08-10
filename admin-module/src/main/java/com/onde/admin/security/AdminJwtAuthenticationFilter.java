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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. 헤더에서 Authorization (Bearer JWT) 추출
        String token = resolveToken(request);

        // 2. 토큰이 있고 검증에 성공한 경우에만 SecurityContext에 인증 정보 저장
        //    토큰이 없거나 검증에 실패하면 인증 없이 다음 필터로 넘기고, 접근 차단은 인가 규칙이 담당
        if (token != null && adminJwtTokenProvider.validateToken(token)) {
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


        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}