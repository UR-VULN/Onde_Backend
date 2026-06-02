package com.onde.api.application.admin;

import com.onde.api.application.admin.dto.MemberAdminResponse;
import com.onde.core.support.ApiResponse;
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
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 전사 회원 리스트 다중 필터 조회 API
     * role, status, name, 가입일(startDate, endDate)을 통한 필터링 지원
     */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'GENERAL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchMembers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<MemberAdminResponse> page = adminMemberService.getMembers(
                role,
                status,
                name != null ? name : keyword,
                startDate,
                endDate,
                pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("members", page.getContent());
        data.put("totalCount", page.getTotalElements());
        data.put("page", page.getNumber());
        data.put("size", page.getSize());
        return ResponseEntity.ok(ApiResponse.success(data, "조회되었습니다."));
    }
}
