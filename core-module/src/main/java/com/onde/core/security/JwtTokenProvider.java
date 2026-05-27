package com.onde.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 실무에서는 application.yml에서 @Value("${jwt.secret}") 로 주입받는 것이 안전합니다.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); 
    private final long accessTokenValidTime = 30 * 60 * 1000L; // 30분 유효기간

    // Access Token 생성 메서드
    public String createAccessToken(String email, String role) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role); // 토큰 Payload에 권한(Role) 정보 담기

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + accessTokenValidTime)) // 만료 시간
                .signWith(key) // 시크릿 키와 암호화 알고리즘 셋팅
                .compact();
    }
}