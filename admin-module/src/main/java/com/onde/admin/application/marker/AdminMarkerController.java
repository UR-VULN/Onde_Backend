package com.onde.admin.application.marker;

import com.onde.admin.application.marker.dto.AdminMarkerRequest;
import com.onde.admin.application.marker.dto.AdminMarkerResponse;
import com.onde.admin.security.LoginAdmin;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/markers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GENERAL_ADMIN')")
public class AdminMarkerController {

    private final AdminMarkerService adminMarkerService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminMarkerResponse>> registerMarker(
            @Valid @RequestBody AdminMarkerRequest req,
            @LoginAdmin String adminId) {

        AdminMarkerResponse response = adminMarkerService.registerMarker(req, adminId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "추천 마커가 등록되었습니다."));
    }
}
