package com.onde.admin.application.notification;

import com.onde.admin.application.notification.dto.BroadcastRequest;
import com.onde.admin.application.notification.dto.BroadcastResponse;
import com.onde.admin.security.LoginAdmin;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BroadcastResponse>> broadcast(
            @Valid @RequestBody BroadcastRequest req,
            @LoginAdmin String adminId) {

        BroadcastResponse response = adminNotificationService.broadcastFcm(req);
        return ResponseEntity.ok(ApiResponse.success(response, "단체 알림 발송이 완료되었습니다."));
    }
}
