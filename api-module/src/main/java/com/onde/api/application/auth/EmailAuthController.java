package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.EmailAuthRequest;
import com.onde.api.application.auth.dto.EmailVerifyRequest;
import com.onde.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<Void> sendVerificationCode(
            @RequestBody EmailAuthRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        emailAuthService.sendVerificationCode(request.getEmail(), userDetails.getMember());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestBody EmailVerifyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, String> tokens = emailAuthService.verifyEmailAndPromote(request.getEmail(), request.getCode(), userDetails.getMember());
        return ResponseEntity.ok(tokens);
    }
}
