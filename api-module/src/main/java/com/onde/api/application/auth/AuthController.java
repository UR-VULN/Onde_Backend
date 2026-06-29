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

@lombok.extern.slf4j.Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.onde.core.security.TokenBlacklistService tokenBlacklistService;
    private final com.onde.core.security.JwtTokenProvider jwtTokenProvider;
    private final com.onde.core.repository.RefreshTokenRepository refreshTokenRepository; // 👈 추가 주입

    @org.springframework.beans.factory.annotation.Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

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
        boolean duplicate = authService.checkNicknameDuplicate(nickname);
        if (duplicate) {
            return ResponseEntity.ok(ApiResponse.success(true, "이미 사용 중인 닉네임입니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "사용 가능한 닉네임입니다."));
    }

    @org.springframework.web.bind.annotation.GetMapping({"/check-email", "/api/v1/auth/check-email"})
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@org.springframework.web.bind.annotation.RequestParam("email") String email) {
        boolean duplicate = authService.checkEmailDuplicate(email);
        if (duplicate) {
            return ResponseEntity.ok(ApiResponse.success(true, "이미 사용 중인 이메일입니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "사용 가능한 이메일입니다."));
    }


    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        LoginResponse loginResponse = authService.login(request, httpServletRequest);

        // Access Token 쿠키 설정 (30분)
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure) // 환경에 따른 HTTPS 통신 제어
                .path("/")
                .sameSite("None") // 크로스 도메인 요청 허용
                .maxAge(30 * 60)
                .build();

        // Refresh Token 쿠키 설정 (14일)
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        // 바디(Body)에도 프론트엔드가 필요한 정보(회원 ID, 권한 등)를 담아서 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResponse, "로그인에 성공하였습니다."));
    }

    @PostMapping("/api/v1/auth/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        LoginResponse loginResponse = authService.adminLogin(request, httpServletRequest);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(30 * 60)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
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
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        
        log.info("[Logout] Logout requested. Authorization header present: {}, Cookies present: {}", 
                authorization != null ? "Yes" : "No", 
                httpServletRequest.getCookies() != null ? "Yes (" + httpServletRequest.getCookies().length + ")" : "No");
        
        String token = extractBearerToken(authorization);
        if (token == null) {
            token = resolveTokenFromCookie(httpServletRequest, "accessToken");
            log.info("[Logout] Extracted token from cookie: {}", token != null ? token.substring(0, Math.min(token.length(), 15)) + "..." : "null");
        } else {
            log.info("[Logout] Extracted token from Authorization header: {}", token.substring(0, Math.min(token.length(), 15)) + "...");
        }

        if (token != null && !token.isBlank()) {
            long remainingTime = jwtTokenProvider.getRemainingExpirationTimeInSeconds(token);
            log.info("[Logout] Token remaining time: {} seconds. Registering to blacklist.", remainingTime);
            tokenBlacklistService.blacklistToken(token, remainingTime);

            // [보안 강화 - WEB-9] 로그아웃 시 Redis 내 저장된 Refresh Token도 완벽 소거(무효화)
            try {
                String email = jwtTokenProvider.getSubject(token);
                if (email != null) {
                    refreshTokenRepository.deleteById(email);
                    log.info("[Logout] Successfully deleted Refresh Token for email: {}", email);
                }
            } catch (Exception e) {
                log.warn("[Logout] Failed to delete Refresh Token during logout: {}", e.getMessage());
            }
        } else {
            log.warn("[Logout] No token found in header or cookie for logout.");
        }

        // 쿠키 삭제 설정
        ResponseCookie deleteAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString())
                .body(ApiResponse.success(null, "로그아웃 되었습니다."));
    }

    private String resolveTokenFromCookie(jakarta.servlet.http.HttpServletRequest request, String cookieName) {
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
