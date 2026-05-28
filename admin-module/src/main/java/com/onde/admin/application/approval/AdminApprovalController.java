package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.AdminApprovalRequest;
import com.onde.admin.application.approval.dto.AdminApprovalResponse;
import com.onde.admin.application.approval.dto.AdminPendingApprovalsResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<AdminPendingApprovalsResponse>> getPendingApprovals(
            @RequestParam(value = "category", required = false) String category,
            @RequestHeader(value = "X-Admin-Role", required = false) String adminRole) {
        
        // Spring Security 역할 격리가 탑재되기 전 로컬 통합 검증을 위한 헤더 모킹
        logAdminRole(adminRole);

        AdminPendingApprovalsResponse response = adminApprovalService.getPendingApprovals(category);
        return ResponseEntity.ok(ApiResponse.success(response, "검수 대기 중인 상품 및 스케줄 목록을 조회했습니다."));
    }

    @PostMapping("/{requestId}")
    public ResponseEntity<ApiResponse<AdminApprovalResponse>> processApproval(
            @PathVariable("requestId") Long requestId,
            @RequestBody AdminApprovalRequest req,
            @RequestHeader(value = "X-Admin-Role", required = false) String adminRole) {
        
        logAdminRole(adminRole);

        AdminApprovalResponse response = adminApprovalService.processApproval(requestId, req);
        String message = String.format("해당 상품의 검수 상태가 %s(으)로 최종 업데이트되었습니다.", response.getDecision().name());
        
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    private void logAdminRole(String role) {
        if (role != null) {
            org.slf4j.LoggerFactory.getLogger(AdminApprovalController.class)
                    .info("🔑 Admin endpoint accessed with role: {}", role);
        }
    }
}
