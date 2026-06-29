package com.onde.admin.application.auth;

import com.onde.admin.application.auth.dto.PasswordChangeRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor

@PreAuthorize("hasAnyRole('SELLER_ADMIN', 'USER_ADMIN', 'SUPER_ADMIN')")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final MemberRepository memberRepository;
    private final com.onde.core.security.TokenBlacklistService tokenBlacklistService;
    private final com.onde.admin.security.AdminJwtTokenProvider adminJwtTokenProvider;
    private final com.onde.core.repository.RefreshTokenRepository refreshTokenRepository; // 👈 추가 주입

    @org.springframework.beans.factory.annotation.Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @jakarta.validation.Valid @RequestBody PasswordChangeRequest request) {
        
        // 시큐리티 컨텍스트에서 현재 로그인한 관리자의 이메일을 가져와 ID 조회
        Member admin = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        adminAuthService.changePassword(admin.getId(), request.getCurrentPassword(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else {
            token = resolveTokenFromCookie(httpServletRequest, "accessToken");
        }

        if (token != null && !token.isBlank()) {
            long remainingTime = adminJwtTokenProvider.getRemainingExpirationTimeInSeconds(token);
            tokenBlacklistService.blacklistToken(token, remainingTime);

            // [보안 강화 - ADMIN-9] 로그아웃 시 Redis 내 저장된 Refresh Token도 완벽 소거(무효화)
            try {
                String email = adminJwtTokenProvider.getClaims(token).getSubject();
                if (email != null) {
                    refreshTokenRepository.deleteById(email);
                }
            } catch (Exception e) {
                // 예외 무시 및 기록 안 함
            }
        }

        // 쿠키 삭제 설정 추가
        org.springframework.http.ResponseCookie deleteAccessCookie = org.springframework.http.ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();

        org.springframework.http.ResponseCookie deleteRefreshCookie = org.springframework.http.ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, deleteAccessCookie.toString())
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString())
                .body(ApiResponse.success(null, "관리자 로그아웃 되었습니다."));
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
}
