package com.onde.api.security.oauth2;

import com.onde.api.application.auth.dto.AuthSessionResult;
import com.onde.api.application.auth.support.AuthCookieSupport;
import com.onde.api.application.auth.support.AuthSessionIssuer;
import com.onde.api.security.CustomUserDetails;
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

    private final AuthSessionIssuer authSessionIssuer;
    private final AuthCookieSupport authCookieSupport;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AuthSessionResult session = authSessionIssuer.issue(userDetails.getMember(), request);

        ResponseCookie accessTokenCookie = authCookieSupport.accessTokenCookie(session.getAccessToken());
        ResponseCookie refreshTokenCookie = authCookieSupport.refreshTokenCookie(session.getRefreshToken());

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/redirect");
    }
}
