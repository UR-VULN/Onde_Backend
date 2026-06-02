package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.EmailAuthRequest;
import com.onde.api.application.auth.dto.EmailVerifyRequest;
import com.onde.api.security.CustomUserDetails;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/email")
@RequiredArgsConstructor
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @RequestBody EmailAuthRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        emailAuthService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "인증번호가 발송되었습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyEmail(
            @RequestBody EmailVerifyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, String> tokens = emailAuthService.verifyEmailAndIssueTokens(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(tokens, "인증이 완료되었습니다."));
    }
}
