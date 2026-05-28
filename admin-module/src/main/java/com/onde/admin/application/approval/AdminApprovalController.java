package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.ApprovalProcessRequest;
import com.onde.admin.application.approval.dto.ApprovalProcessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {
    private final AdminApprovalService adminApprovalService;

    @PostMapping("/process")
    public ResponseEntity<ApprovalProcessResponse> processApproval(@RequestBody ApprovalProcessRequest request) {
        return ResponseEntity.ok(adminApprovalService.processApproval(request));
    }
}
