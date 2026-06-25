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

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;
    
    private final Map<String, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();

    private void checkRateLimitAndBan(String clientIp, String actionType) {
        String banKey = "BAN:AUTH_" + actionType + ":" + clientIp;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        Bucket bucket = rateLimitBuckets.computeIfAbsent(clientIp + ":" + actionType, k -> 
            Bucket.builder().addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))).build()
        );

        if (!bucket.tryConsume(1)) {
            redisTemplate.opsForValue().set(banKey, "BANNED", 1, TimeUnit.HOURS);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }


    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
        }
        checkRateLimitAndBan(clientIp, "SIGNUP");

        SignupResponse result = authService.signup(request);
        String message = result.getRole() == MemberRole.SELLER && result.getStatus() == MemberStatus.PENDING
                 ? "판매자 회원가입이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
                 : "회원가입이 완료되었습니다.";
        return ResponseEntity
                 .status(HttpStatus.CREATED)
                 .body(ApiResponse.success(result, message));
    }

    @org.springframework.web.bind.annotation.GetMapping({"/check-nickname", "/api/v1/auth/check-nickname"})
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@org.springframework.web.bind.annotation.RequestParam("nickname") String nickname, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
        }
        checkRateLimitAndBan(clientIp, "NICKNAME");

        boolean duplicate = authService.checkNicknameDuplicate(nickname, clientIp);
        // 응답 메시지를 통일하여 길이/메시지 기반 Enumeration을 완화
        return ResponseEntity.ok(ApiResponse.success(duplicate, "닉네임 중복 확인 결과입니다."));
    }

    @org.springframework.web.bind.annotation.GetMapping({"/check-email", "/api/v1/auth/check-email"})
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@org.springframework.web.bind.annotation.RequestParam("email") String email, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
        }
        checkRateLimitAndBan(clientIp, "EMAIL");

        boolean duplicate = authService.checkEmailDuplicate(email, clientIp);
        // 응답 데이터 포맷을 동일하게 유지
        return ResponseEntity.ok(ApiResponse.success(duplicate, "이메일 중복 확인 결과입니다."));
    }


    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        LoginResponse loginResponse = authService.login(request, clientIp);

        String serverName = httpRequest.getServerName();
        boolean isLocal = "localhost".equals(serverName) || "127.0.0.1".equals(serverName) || "api".equals(serverName) || "admin".equals(serverName);

        ResponseCookie.ResponseCookieBuilder accessTokenBuilder = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Strict") // CSRF 방어용 Strict
                .maxAge(30 * 60);

        ResponseCookie.ResponseCookieBuilder refreshTokenBuilder = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(14 * 24 * 60 * 60);

        if (!isLocal) {
            accessTokenBuilder.secure(true).domain("onde.click");
            refreshTokenBuilder.secure(true).domain("onde.click");
        }

        ResponseCookie accessTokenCookie = accessTokenBuilder.build();
        ResponseCookie refreshTokenCookie = refreshTokenBuilder.build();

        // 바디(Body)에도 프론트엔드가 필요한 정보(회원 ID, 권한 등)를 담아서 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResponse, "로그인에 성공하였습니다."));
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

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
