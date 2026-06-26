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
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_JTI = "jti";

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;

    private final long accessTokenValidTime = 30 * 60 * 1000L;
    private final long refreshTokenValidTime = 14 * 24 * 60 * 60 * 1000L;

    @PostConstruct
    public void init() {
        String validatedSecret = RequiredSecretValidator.requireJwtSecret(secretKey);
        this.key = Keys.hmacShaKeyFor(validatedSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** @param subject 비식별 주체 ID (Member.authSubjectId) */
    public String createAccessToken(String subject, String role) {
        Date now = new Date();
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_JTI, UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenValidTime))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(key)
                .compact();
    }

    public long getRefreshTokenValidTimeInSeconds() {
        return refreshTokenValidTime / 1000;
    }

    public long getAccessTokenValidTimeInSeconds() {
        return accessTokenValidTime / 1000;
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String getJti(String token) {
        Object jti = parseClaims(token).get(CLAIM_JTI);
        return jti != null ? jti.toString() : null;
    }

    public long getRemainingValiditySeconds(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long remainingMs = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMs / 1000);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
