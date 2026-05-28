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
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());

            UserDetails principal = new User(email, "", authorities);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } 
        // 3. [우선순위 2] 토큰이 없거나 무효하지만, 테스트용 HTTP 헤더 정보가 넘어온 경우 (완전 수용)
        else {
            String adminId = request.getHeader("X-Admin-Id");
            String adminRole = request.getHeader("X-Admin-Role");

            if (adminId != null && !adminId.isBlank() && adminRole != null && !adminRole.isBlank()) {
                // 권한 식별자 포맷 통일 (ROLE_ADMIN, ROLE_SUPER_ADMIN 등)
                String roleName = adminRole.toUpperCase().startsWith("ROLE_") ? adminRole.toUpperCase() : "ROLE_" + adminRole.toUpperCase();
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));
                
                // 첫 번째 필터와 두 번째 필터의 Principal 타입을 UserDetails(User) 구체 클래스로 통일하여 캐스팅 에러 방지
                UserDetails principal = new User(adminId, "", authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                
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
}