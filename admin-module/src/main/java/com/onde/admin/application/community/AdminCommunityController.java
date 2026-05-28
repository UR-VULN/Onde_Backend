package com.onde.admin.application.community;

import com.onde.admin.application.community.dto.AdminBlindRequest;
import com.onde.admin.application.community.dto.AdminBlindResponse;
import com.onde.admin.security.LoginAdmin;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    @PatchMapping("/{postId}/blind")
    public ResponseEntity<ApiResponse<AdminBlindResponse>> blindPost(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody AdminBlindRequest req,
            @LoginAdmin String adminId) {

        AdminBlindResponse response = adminCommunityService.blindPost(postId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 블라인드 처리되었습니다."));
    }

    @PatchMapping("/{postId}/restore")
    public ResponseEntity<ApiResponse<AdminBlindResponse>> restorePost(
            @PathVariable("postId") Long postId,
            @LoginAdmin String adminId) {

        AdminBlindResponse response = adminCommunityService.restorePost(postId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 정상 복구되었습니다."));
    }
}
