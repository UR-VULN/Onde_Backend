package com.onde.api.application.admin;

import com.onde.api.application.admin.AdminMemberService;
import com.onde.api.application.admin.dto.MemberAdminResponse;
import com.onde.api.application.admin.dto.MemberSearchRequest;
import com.onde.api.application.admin.dto.RoleUpdateRequest;
import com.onde.core.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final MemberRepository memberRepository;

    // 전사 회원 리스트 다중 필터 (GET)
    @GetMapping
    public ResponseEntity<Page<MemberAdminResponse>> searchMembers(
            @ModelAttribute MemberSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<MemberAdminResponse> response = adminMemberService.getMembers(request, pageable);
        return ResponseEntity.ok(response);
    }

    // 회원 블랙리스트 처리 (POST)
    @PostMapping("/{id}/blacklist")
    public ResponseEntity<String> blacklistMember(@PathVariable Long id) {
        
        adminMemberService.blacklistMember(id);
        return ResponseEntity.ok("해당 회원이 블랙리스트로 지정되었으며, 강제 로그아웃 처리되었습니다.");
    }

    @PatchMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<String> updateRole(@PathVariable Long id, 
                                             @RequestBody RoleUpdateRequest request,
                                             Principal principal) {
        Long currentAdminId = getAdminIdFromPrincipal(principal);
        adminMemberService.updateMemberRole(id, currentAdminId, request.getRole());
        return ResponseEntity.ok("권한이 성공적으로 변경되었습니다.");
    }

    private Long getAdminIdFromPrincipal(Principal principal) {
        return memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."))
                .getId();
    }
}