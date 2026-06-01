package com.onde.api.application.admin;

import com.onde.api.application.admin.dto.MemberAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 전사 회원 리스트 다중 필터 조회 API
     * role, 가입일(startDate, endDate), 검색어(keyword)를 통한 필터링 지원
     */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<MemberAdminResponse>> searchMembers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<MemberAdminResponse> response = adminMemberService.getMembers(role, keyword, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }
}
