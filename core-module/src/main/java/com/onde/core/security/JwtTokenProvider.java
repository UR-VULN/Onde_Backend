package com.onde.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    
    private Key key;

    private final long accessTokenValidTime = 15 * 60 * 1000L; // 15분 유효기간
    private final long refreshTokenValidTime = 14 * 24 * 60 * 60 * 1000L; // 14일 유효기간

    // 서버 실행 시 환경변수의 키를 바이트 배열로 변환하여 Key 객체 초기화
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    // Access Token 생성 메서드
    public String createAccessToken(String identifier) {
        Claims claims = Jwts.claims().setSubject(identifier);

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
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}