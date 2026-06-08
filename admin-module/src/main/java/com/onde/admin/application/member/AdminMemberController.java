package com.onde.admin.application.member;

import com.onde.admin.application.member.dto.BlacklistRequest;
import com.onde.admin.application.member.dto.MemberAdminResponse;
import com.onde.admin.application.member.dto.MemberSearchRequest;
import com.onde.admin.application.member.dto.MemberStatusUpdateRequest;
import com.onde.admin.application.member.dto.RoleUpdateRequest;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.repository.MemberRepository;
import com.onde.core.support.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final MemberRepository memberRepository;

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchMembers(
            @ModelAttribute MemberSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<MemberAdminResponse> page = adminMemberService.getMembers(request, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("members", page.getContent());
        data.put("totalCount", page.getTotalElements());
        data.put("page", page.getNumber());
        data.put("size", page.getSize());
        return ResponseEntity.ok(ApiResponse.success(data, "조회되었습니다."));
    }

    @PostMapping("/members/{id}/blacklist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blacklistMember(
            @PathVariable Long id,
            @RequestBody(required = false) BlacklistRequest request) {
        
        adminMemberService.blacklistMember(id, request != null ? request.getReason() : null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberId", id);
        data.put("role", MemberRole.BLACKLIST.name());
        data.put("blacklistedAt", LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(data, "블랙리스트 처리되었습니다."));
    }

    @PatchMapping("/roles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRole(@PathVariable Long id,
                                             @RequestBody RoleUpdateRequest request,
                                             Principal principal) {
        Long currentAdminId = getAdminIdFromPrincipal(principal);
        MemberRole appliedRole = adminMemberService.updateMemberRole(id, currentAdminId, request.resolvePrimaryRole());
        Member member = memberRepository.findById(id).orElseThrow();
        List<String> roles = request.getRoles() != null && !request.getRoles().isEmpty()
                ? request.getRoles().stream().map(MemberRole::name).toList()
                : List.of(appliedRole.name());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberId", id);
        data.put("email", member.getEmail());
        data.put("roles", roles);
        data.put("updatedAt", LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(data, "권한이 수정되었습니다."));
    }

    @PatchMapping("/members/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable Long id,
            @RequestBody MemberStatusUpdateRequest request) {

        MemberStatus appliedStatus = adminMemberService.updateMemberStatus(id, request.getStatus());
        Member member = memberRepository.findById(id).orElseThrow();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberId", id);
        data.put("email", member.getEmail());
        data.put("role", member.getRole().name());
        data.put("status", appliedStatus.name());
        data.put("updatedAt", LocalDateTime.now());

        String message = appliedStatus == MemberStatus.ACTIVE && member.getRole() == MemberRole.SELLER
                ? "판매자 계정이 승인되었습니다."
                : "회원 상태가 수정되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    private Long getAdminIdFromPrincipal(Principal principal) {
        return memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."))
                .getId();
    }
}
