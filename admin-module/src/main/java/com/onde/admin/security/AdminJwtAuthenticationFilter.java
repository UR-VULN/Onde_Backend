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

        // 2. [우선순위 1] 진짜 JWT 토큰이 넘어왔고 검증이 성공한 경우
        if (token != null && adminJwtTokenProvider.validateToken(token)) {
            Claims claims = adminJwtTokenProvider.getClaims(token);
            String email = claims.getSubject();
            List<String> rolesList = extractRoles(claims);

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

    private List<String> extractRoles(Claims claims) {
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List<?> rawRoles) {
            return rawRoles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }

        String singleRole = claims.get("role", String.class);
        if (singleRole != null) {
            return List.of(singleRole);
        }
        return List.of();
    }
}