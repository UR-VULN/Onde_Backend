package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.AdminApprovalRequest;
import com.onde.admin.application.approval.dto.AdminApprovalResponse;
import com.onde.admin.application.approval.dto.AdminPendingApprovalsResponse;
import com.onde.admin.application.approval.dto.ApprovalProcessRequest;
import com.onde.admin.application.approval.dto.ApprovalProcessResponse;
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
@PreAuthorize("hasAnyRole('SALES_ADMIN', 'GENERAL_ADMIN', 'SUPER_ADMIN')")
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    /**
     * 검수 대기 중인 상품 및 스케줄 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<AdminPendingApprovalsResponse>> getPendingApprovals(
            @RequestParam(value = "category", required = false) String category) {

        log.info("🔑 Admin endpoint accessed for pending approvals");

        AdminPendingApprovalsResponse response = adminApprovalService.getPendingApprovals(category);
        return ResponseEntity.ok(ApiResponse.success(response, "검수 대기 중인 상품 및 스케줄 목록을 조회했습니다."));
    }

    /**
     * [첫 번째 코드 스펙] Request Body DTO 기반 승인 처리
     * 숙소,렌터카 승인
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<ApprovalProcessResponse>> processApproval(
            @RequestBody ApprovalProcessRequest request) {

        log.info("🔑 Admin endpoint accessed: /process");

        ApprovalProcessResponse response = adminApprovalService.processApproval(request);
        return ResponseEntity.ok(ApiResponse.success(response, "검수 처리가 완료되었습니다."));
    }

    /**
     * [두 번째 코드 스펙] PathVariable(requestId) 기반 승인 처리
     * 항공, 보험 승인
     */
    @PostMapping("/{requestId}")
    public ResponseEntity<ApiResponse<AdminApprovalResponse>> processApproval(
            @PathVariable("requestId") Long requestId,
            @RequestBody AdminApprovalRequest req) {

        log.info("🔑 Admin endpoint accessed: /{}", requestId);

        AdminApprovalResponse response = adminApprovalService.processApproval(requestId, req);
        String message = String.format("해당 상품의 검수 상태가 %s(으)로 최종 업데이트되었습니다.", response.getStatus().name());

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
