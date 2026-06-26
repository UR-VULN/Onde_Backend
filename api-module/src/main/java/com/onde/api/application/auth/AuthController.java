package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.*;
import com.onde.api.application.auth.support.AuthCookieSupport;
import com.onde.core.support.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.onde.core.validation.ValidationLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieSupport authCookieSupport;

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse result = authService.signup(request);
        String message = result.getRole() == com.onde.core.entity.member.MemberRole.SELLER
                && result.getStatus() == com.onde.core.entity.member.MemberStatus.PENDING
                ? "판매자 회원가입이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
                : "회원가입이 완료되었습니다.";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, message));
    }

    @GetMapping({"/check-nickname", "/api/v1/auth/check-nickname"})
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(
            @RequestParam("nickname")
            @NotBlank(message = "닉네임은 필수입니다.")
            @Size(min = ValidationLimits.NICKNAME_MIN, max = ValidationLimits.NICKNAME_MAX, message = "닉네임은 2~30자여야 합니다.")
            String nickname) {
        boolean duplicate = authService.checkNicknameDuplicate(nickname);
        if (duplicate) {
            return ResponseEntity.ok(ApiResponse.success(true, "이미 사용 중인 닉네임입니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "사용 가능한 닉네임입니다."));
    }

    @GetMapping({"/check-email", "/api/v1/auth/check-email"})
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam("email")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            @Size(max = ValidationLimits.EMAIL_MAX, message = "이메일은 320자 이하여야 합니다.")
            String email) {
        boolean duplicate = authService.checkEmailDuplicate(email);
        if (duplicate) {
            return ResponseEntity.ok(ApiResponse.success(true, "이미 사용 중인 이메일입니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "사용 가능한 이메일입니다."));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        AuthSessionResult session = authService.login(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.accessTokenCookie(session.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.refreshTokenCookie(session.getRefreshToken()).toString())
                .body(ApiResponse.success(session.getProfile(), "로그인에 성공하였습니다."));
    }

    @PostMapping("/api/v1/auth/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        AuthSessionResult session = authService.adminLogin(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.accessTokenCookie(session.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.refreshTokenCookie(session.getRefreshToken()).toString())
                .body(ApiResponse.success(session.getProfile(), "관리자 로그인에 성공하였습니다."));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String accessToken = resolveTokenFromCookie(request, "accessToken");
        String refreshToken = resolveTokenFromCookie(request, "refreshToken");
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.clearRefreshTokenCookie().toString())
                .body(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(HttpServletRequest servletRequest) {
        String refreshToken = resolveTokenFromCookie(servletRequest, "refreshToken");
        return buildRefreshResponse(refreshToken, servletRequest);
    }

    private ResponseEntity<ApiResponse<TokenRefreshResponse>> buildRefreshResponse(
            String refreshToken,
            HttpServletRequest servletRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }
        RefreshSessionResult session = authService.refresh(refreshToken, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.accessTokenCookie(session.getAccessToken()).toString())
                .body(ApiResponse.success(session.getProfile(), "Access Token이 재발급되었습니다."));
    }

    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
