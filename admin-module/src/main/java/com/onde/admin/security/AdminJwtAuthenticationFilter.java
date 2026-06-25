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
    private final com.onde.core.repository.MemberRepository memberRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. 헤더에서 Authorization (Bearer JWT) 추출
        String token = resolveToken(request);

        // 2. [우선순위 1] 진짜 JWT 토큰이 넘어왔고 검증이 성공한 경우
        if (token != null && adminJwtTokenProvider.validateToken(token)) {
            try {
                String identifier = adminJwtTokenProvider.getClaims(token).getSubject();
                com.onde.core.entity.member.Member member = memberRepository.findById(Long.parseLong(identifier))
                        .orElse(null);

                if (member != null) {
                    // [단일 세션 정책] Redis에서 현재 활성 상태인 Access Token과 일치하는지 검증
                    String activeToken = redisTemplate.opsForValue().get("active_access_token:" + identifier);
                    if (activeToken == null || !activeToken.equals(token)) {
                        // 다른 기기에서 로그인됨 -> 무효화
                    } else {
                        String role = member.getRole().getSecurityRole();
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                        org.springframework.security.core.Authentication authentication = new UsernamePasswordAuthenticationToken(
                                member.getId(), null, authorities);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Parse error or DB error, continue without auth
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