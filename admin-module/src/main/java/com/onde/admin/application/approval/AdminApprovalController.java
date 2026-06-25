package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.*;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER_ADMIN', 'USER_ADMIN', 'SUPER_ADMIN')")
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    /**
     * 검수 대기 중인 상품 및 스케줄 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<AdminPendingApprovalsResponse>> getPendingApprovals(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(adminApprovalService.getPendingApprovals(category), "조회 완료"));
    }

    /**
     * Request Body DTO 기반 승인 처리
     * 숙소,렌터카 승인
     */
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ApprovalProcessResponse>> processApproval(
            @RequestBody ApprovalProcessRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminApprovalService.processApproval(request), "처리 완료"));
    }

    /**
     * PathVariable(requestId) 기반 승인 처리
     * 항공, 보험 승인
     */
    @PostMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminApprovalResponse>> processApproval(
            @PathVariable("requestId") Long requestId,
            @RequestBody AdminApprovalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminApprovalService.processApproval(requestId, req), "처리 완료"));
    }
}
