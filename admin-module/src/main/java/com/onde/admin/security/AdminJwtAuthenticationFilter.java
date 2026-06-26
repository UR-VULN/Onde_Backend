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
    private final AdminTokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. 헤더에서 Authorization (Bearer JWT) 추출
        String token = resolveToken(request);

        // 2. 유효한 토큰이고 블랙리스트에 등록되지 않은 경우에만 인증 처리
        if (token != null && adminJwtTokenProvider.validateToken(token) && !tokenBlacklistService.isBlacklisted(token)) {
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