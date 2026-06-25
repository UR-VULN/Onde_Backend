package com.onde.admin.application.auth;

import com.onde.admin.application.auth.dto.PasswordChangeRequest;
import com.onde.api.application.auth.dto.LoginRequest; 
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.security.JwtTokenProvider; 
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider; 

    @PostMapping("/login")
    @PreAuthorize("permitAll()") 
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        // 1. 서비스에서 인증 및 토큰 생성(또는 토큰 provider 사용)
        Member admin = adminAuthService.authenticate(request.getEmail(), request.getPassword());
        String accessToken = jwtTokenProvider.createAccessToken(admin.getEmail(), admin.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getEmail());

        // 2. 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true).secure(true).path("/").sameSite("Lax").maxAge(30 * 60).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true).secure(true).path("/").sameSite("Lax").maxAge(14 * 24 * 60 * 60).build();

        Map<String, Object> data = new HashMap<>();
        data.put("email", admin.getEmail());
        data.put("role", admin.getRole().name()); // .name() 추가
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(data, "로그인 성공"));
    }

    // 비밀번호 변경은 권한 필요
    @PostMapping("/password-change")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'USER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordChangeRequest request) {
        
        Member admin = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        adminAuthService.changePassword(admin.getId(), request.getCurrentPassword(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
    }
}
