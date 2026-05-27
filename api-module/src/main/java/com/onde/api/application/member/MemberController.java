package com.onde.api.application.member;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.support.ApiResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Member>> createTestMember(@RequestBody TestMemberRequest req) {
        Member member = memberService.createTestMember(req.getId(), req.getName(), req.getRole());
        return ResponseEntity.ok(ApiResponse.success(member, "테스트용 회원이 정상 생성되었습니다."));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestMemberRequest {
        private Long id;
        private String name;
        private MemberRole role;
    }
}
