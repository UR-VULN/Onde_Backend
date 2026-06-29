package com.onde.api.application.member;

import com.onde.api.application.member.dto.MemberMeResponse;
import com.onde.api.security.CustomUserDetails;
import com.onde.core.entity.member.Member;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        
        // SecurityContext에서 인증된 유저 엔티티 추출
        Member member = userDetails.getMember();

        MemberMeResponse response = MemberMeResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .role(member.getRole().name())
                .status(member.getStatus().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보 조회 성공"));
    }
}