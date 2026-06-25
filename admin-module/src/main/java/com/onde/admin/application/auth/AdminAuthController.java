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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import com.onde.admin.application.auth.dto.AdminLoginRequest;
import com.onde.admin.application.auth.dto.AdminLoginResponse;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor

@PreAuthorize("hasAnyRole('SELLER_ADMIN', 'USER_ADMIN', 'SUPER_ADMIN') or permitAll()")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final MemberRepository memberRepository;

    @PreAuthorize("permitAll()")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @jakarta.validation.Valid @RequestBody AdminLoginRequest request, 
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String clientIp = httpRequest.getRemoteAddr();
        AdminLoginResponse loginResponse = adminAuthService.login(request, clientIp);

        String serverName = httpRequest.getServerName();
        boolean isLocal = "localhost".equals(serverName) || "127.0.0.1".equals(serverName) || "api".equals(serverName) || "admin".equals(serverName);

        ResponseCookie.ResponseCookieBuilder accessTokenBuilder = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(30 * 60);

        ResponseCookie.ResponseCookieBuilder refreshTokenBuilder = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(14 * 24 * 60 * 60);

        if (!isLocal) {
            accessTokenBuilder.secure(true).domain("admin.onde.click");
            refreshTokenBuilder.secure(true).domain("admin.onde.click");
        }

        ResponseCookie accessTokenCookie = accessTokenBuilder.build();
        ResponseCookie refreshTokenCookie = refreshTokenBuilder.build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(loginResponse, "관리자 로그인에 성공하였습니다."));
    }

    @PostMapping("/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordChangeRequest request) {
        
        // 시큐리티 컨텍스트에서 현재 로그인한 관리자의 이메일을 가져와 ID 조회
        Member admin = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        adminAuthService.changePassword(admin.getId(), request.getCurrentPassword(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
    }
}
