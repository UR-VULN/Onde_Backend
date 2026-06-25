package com.onde.admin.application.marker;

import com.onde.admin.application.marker.dto.AdminMarkerRequest;
import com.onde.admin.application.marker.dto.AdminMarkerResponse;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; 
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/markers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
public class AdminMarkerController {

    private final AdminMarkerService adminMarkerService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminMarkerResponse>> registerMarker(
            @Valid @RequestBody AdminMarkerRequest req,
            // 변조 위험이 있는 헤더값 대신 JWT 검증이 완료된 Authentication 객체 주입
            Authentication authentication) {

        // 서버에서 안전하게 로그인한 사용자의 ID(식별자)를 가져옴
        String adminId = authentication.getName();

        AdminMarkerResponse response = adminMarkerService.registerMarker(req, adminId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "추천 마커가 등록되었습니다."));
    }
}
