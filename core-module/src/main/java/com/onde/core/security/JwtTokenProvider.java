package com.onde.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    
    private Key key;

    private final long accessTokenValidTime = 30 * 60 * 1000L; // 30분 유효기간
    private final long refreshTokenValidTime = 14 * 24 * 60 * 60 * 1000L; // 14일 유효기간

    // 서버 실행 시 환경변수의 키를 바이트 배열로 변환하여 Key 객체 초기화
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    // Access Token 생성 메서드
    public String createAccessToken(String identifier, String role) {
        Claims claims = Jwts.claims().setSubject(identifier);
        claims.put("role", role); // 토큰 Payload에 권한(Role) 정보 담기

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + accessTokenValidTime)) // 만료 시간
                .signWith(key) // 시크릿 키와 암호화 알고리즘 셋팅
                .compact();
    }

    public String createRefreshToken(String identifier) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(identifier)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(key)
                .compact();
    }

    // Redis TTL 설정을 위해 초 단위로 반환하는 유틸리티 메서드
    public long getRefreshTokenValidTimeInSeconds() {
        return refreshTokenValidTime / 1000;
    }

    // 토큰의 subject(email 혹은 providerId)를 꺼내는 메서드
    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰의 유효성 및 만료일자 확인
    public boolean validateToken(String token) {
        try {
            // parseClaimsJws는 토큰이 서명되지 않았거나(alg=none), 서명이 다르면 즉시 예외를 발생시킵니다.
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("[보안 경고] 잘못된 JWT 서명입니다. 위조 시도 가능성: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("[보안 경고] 손상된 JWT 토큰입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("[인증 정보] 만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("[보안 경고] 지원하지 않는 JWT 토큰 형식입니다 (alg=none 의심): {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[보안 경고] JWT 클레임이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }
}