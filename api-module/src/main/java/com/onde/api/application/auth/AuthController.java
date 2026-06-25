package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.LoginRequest;
import com.onde.api.application.auth.dto.LoginResponse;
import com.onde.api.application.auth.dto.SignupRequest;
import com.onde.api.application.auth.dto.SignupResponse;
import com.onde.api.application.auth.dto.TokenRefreshRequest;
import com.onde.api.application.auth.dto.TokenRefreshResponse;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse result = authService.signup(request);
        String message = result.getRole() == MemberRole.SELLER && result.getStatus() == MemberStatus.PENDING
                 ? "판매자 회원가입이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
                 : "회원가입이 완료되었습니다.";
        return ResponseEntity
                 .status(HttpStatus.CREATED)
                 .body(ApiResponse.success(result, message));
    }

    @org.springframework.web.bind.annotation.GetMapping({"/check-nickname", "/api/v1/auth/check-nickname"})
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@org.springframework.web.bind.annotation.RequestParam("nickname") String nickname) {
        // [보안 패치] User Enumeration 방어를 위해 항상 동일한 응답 반환
        return ResponseEntity.ok(ApiResponse.success(false, "요청이 처리되었습니다."));
    }

    @org.springframework.web.bind.annotation.GetMapping({"/check-email", "/api/v1/auth/check-email"})
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@org.springframework.web.bind.annotation.RequestParam("email") String email) {
        // [보안 패치] User Enumeration 방어를 위해 항상 동일한 응답 반환
        return ResponseEntity.ok(ApiResponse.success(false, "요청이 처리되었습니다."));
    }


    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);

        // Access Token 쿠키 설정 (30분) - 로컬 개발을 위해 secure(false), sameSite("Lax") 적용
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(30 * 60)
                .build();

        // Refresh Token 쿠키 설정 (14일)
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        // 바디(Body)에도 프론트엔드가 필요한 정보(회원 ID, 권한 등)를 담아서 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResponse, "로그인에 성공하였습니다."));
    }

    @PostMapping("/api/v1/auth/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.adminLogin(request);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(30 * 60)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResponse, "관리자 로그인에 성공하였습니다."));
    }

    @PostMapping(value = "/api/v1/auth/refresh", headers = HttpHeaders.AUTHORIZATION)
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshWithAuthorizationHeader(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String refreshToken = extractBearerToken(authorization);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }

        TokenRefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Access Token이 재발급되었습니다."));
    }

    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) TokenRefreshRequest request) {
        String refreshToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            refreshToken = extractBearerToken(authorization);
        } else if (request != null) {
            refreshToken = request.getRefreshToken();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }

        TokenRefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Access Token이 재발급되었습니다."));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request,
            java.security.Principal principal) {
        
        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        } else {
            accessToken = resolveCookieValue(request, "accessToken");
        }

        if (principal != null) {
            authService.logout(principal.getName(), accessToken);
        }

        // Access Token 쿠키 즉시 만료
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        // Refresh Token 쿠키 즉시 만료
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(null, "로그아웃 되었습니다."));
    }

    private String resolveCookieValue(jakarta.servlet.http.HttpServletRequest request, String cookieName) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
