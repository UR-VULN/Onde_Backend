package com.onde.admin.application.auth;

import com.onde.admin.application.auth.dto.PasswordChangeRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
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

    @PostMapping("/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {
        
        // 시큐리티 컨텍스트에서 현재 로그인한 관리자의 이메일을 가져와 ID 조회
        Member admin = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        adminAuthService.changePassword(admin.getId(), request.getCurrentPassword(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
    }
}
