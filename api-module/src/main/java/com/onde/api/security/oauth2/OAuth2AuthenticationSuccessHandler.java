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

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 1. JWT 토큰 발행
        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        String accessToken = jwtTokenProvider.createAccessToken(userDetails.getUsername(), authority);
        String refreshTokenString = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

        // 2. Redis에 Refresh Token 세이브
        RefreshToken refreshToken = new RefreshToken(
                userDetails.getUsername(),
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // 3. 브라우저 보안 쿠키 베이킹 (HttpOnly, Secure)
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true).secure(true).path("/").sameSite("None").maxAge(30 * 60).build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshTokenString)
                .httpOnly(true).secure(true).path("/").sameSite("None").maxAge(14 * 24 * 60 * 60).build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        
        // 4. 정상 메인 페이지로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/redirect");
    }
}