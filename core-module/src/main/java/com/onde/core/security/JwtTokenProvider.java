package com.onde.core.security;

import io.jsonwebtoken.*;
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

    // 토큰의 유효성 및 만료일자 및 알고리즘 변조 여부확인
    public boolean validateToken(String token) {
        try {
            // parseClaimsJws는 서명이 존재하지 않거나(alg=none), 서명이 일치하지 않으면 즉각 예외를 발생시킴
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | SignatureException e) {
            log.error("[Security] 잘못된 JWT 서명입니다. (변조 공격 의심): {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("[Security] 유효하지 않은 구조의 JWT 토큰입니다. (구조 훼손): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("[Security] 만료된 JWT 토큰입니다.: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // alg=none 공격 등 서버가 허용하지 않는 형식의 토큰이 들어왔을 때 여기서 차단됩니다.
            log.error("[Security] 지원되지 않는 형식의 JWT 토큰입니다. (alg=none 공격 등 의심): {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[Security] JWT 토큰이 비어있거나 잘못되었습니다.: {}", e.getMessage());
        }
        return false;
    }
}