package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.LoginRequest;
import com.onde.api.application.auth.dto.LoginResponse;
import com.onde.api.application.auth.dto.SignupRequest;
import com.onde.api.application.auth.dto.SignupRequest;
import com.onde.api.application.auth.dto.TokenRefreshRequest;
import com.onde.api.application.auth.dto.TokenRefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        String result = authService.signup(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);

        // Access Token 쿠키 설정 (30분)
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(true) // HTTPS 통신 강제
                .path("/")
                .sameSite("None") // 크로스 도메인 요청 허용
                .maxAge(30 * 60)
                .build();

        // Refresh Token 쿠키 설정 (14일)
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        // 바디(Body)에도 프론트엔드가 필요한 정보(회원 ID, 권한 등)를 담아서 응답합니다.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}