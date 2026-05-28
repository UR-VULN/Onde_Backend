package com.onde.admin.application.notification;

import com.onde.admin.application.notification.dto.BroadcastRequest;
import com.onde.admin.application.notification.dto.BroadcastResponse;
import com.onde.admin.security.LoginAdmin;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.support.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<BroadcastResponse>> broadcast(
            @Valid @RequestBody BroadcastRequest req,
            @LoginAdmin String adminId,
            HttpServletRequest httpServletRequest) {

        // SUPER_ADMIN 전용 역할 검증
        String adminRole = httpServletRequest.getHeader("X-Admin-Role");
        if (adminRole == null || !adminRole.equals("SUPER_ADMIN")) {
            throw new ForbiddenException(ErrorCode.POST_NOT_OWNER); // 권한 없음 예외 처리
        }

        BroadcastResponse response = adminNotificationService.broadcastFcm(req);
        return ResponseEntity.ok(ApiResponse.success(response, "단체 알림 발송이 완료되었습니다."));
    }
}
