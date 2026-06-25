package com.onde.api.security.oauth2;

import com.onde.api.security.CustomUserDetails;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 1. JWT 토큰 발행
        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        String memberIdStr = String.valueOf(userDetails.getMember().getId());
        String accessToken = jwtTokenProvider.createAccessToken(memberIdStr);
        String refreshTokenString = jwtTokenProvider.createRefreshToken(memberIdStr);

        // 2. Redis에 Refresh Token 세이브
        RefreshToken refreshToken = new RefreshToken(
                memberIdStr,
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // [단일 세션 정책] Access Token을 Redis에 저장
        redisTemplate.opsForValue().set(
                "active_access_token:" + memberIdStr,
                accessToken,
                900L,
                java.util.concurrent.TimeUnit.SECONDS
        );

        // 3. 브라우저 보안 쿠키 베이킹
        String serverName = request.getServerName();
        boolean isLocal = "localhost".equals(serverName) || "127.0.0.1".equals(serverName) || "api".equals(serverName) || "admin".equals(serverName);

        ResponseCookie.ResponseCookieBuilder accessTokenBuilder = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(30 * 60);

        ResponseCookie.ResponseCookieBuilder refreshTokenBuilder = ResponseCookie.from("refreshToken", refreshTokenString)
                .httpOnly(true)
                .path("/")
                .maxAge(14 * 24 * 60 * 60);

        if (!isLocal) {
            accessTokenBuilder.secure(true).sameSite("None");
            refreshTokenBuilder.secure(true).sameSite("None");
        } else {
            accessTokenBuilder.sameSite("Lax");
            refreshTokenBuilder.sameSite("Lax");
        }

        ResponseCookie accessTokenCookie = accessTokenBuilder.build();
        ResponseCookie refreshTokenCookie = refreshTokenBuilder.build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        
        if ("ROLE_GUEST".equals(authority)) {
            // 이메일 추가 수집 페이지로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/signup/email");
        } else {
            // 정상 메인 페이지로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/redirect");
        }
    }
}