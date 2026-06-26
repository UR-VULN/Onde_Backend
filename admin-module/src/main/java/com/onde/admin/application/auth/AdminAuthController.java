package com.onde.admin.application.auth;

import com.onde.admin.application.auth.dto.PasswordChangeRequest;
import com.onde.admin.security.AdminJwtTokenProvider;
import com.onde.admin.security.AdminTokenBlacklistService;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.support.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final AdminTokenBlacklistService tokenBlacklistService;

    @PostMapping("/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordChangeRequest request) {

        Member admin = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        adminAuthService.changePassword(admin.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (adminJwtTokenProvider.validateToken(token)) {
                Claims claims = adminJwtTokenProvider.getClaims(token);
                long remainingSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                tokenBlacklistService.blacklist(token, remainingSeconds);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }
}
