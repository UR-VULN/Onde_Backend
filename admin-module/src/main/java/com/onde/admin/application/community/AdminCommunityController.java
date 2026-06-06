package com.onde.admin.application.community;

import com.onde.admin.application.community.dto.AdminBlindRequest;
import com.onde.admin.application.community.dto.AdminBlindResponse;
import com.onde.admin.security.LoginAdmin;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.onde.admin.application.community.dto.AdminPostDetailResponse>>> getPosts(
            @RequestParam(value = "status", required = false) com.onde.core.entity.community.PostStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @LoginAdmin String adminId) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<com.onde.admin.application.community.dto.AdminPostDetailResponse> response =
                adminCommunityService.getAdminPosts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{postId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<com.onde.admin.application.community.dto.AdminPostDetailResponse>> getPostDetail(
            @PathVariable("postId") Long postId,
            @LoginAdmin String adminId) {

        com.onde.admin.application.community.dto.AdminPostDetailResponse response = adminCommunityService.getAdminPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable("postId") Long postId,
            @LoginAdmin String adminId) {

        adminCommunityService.forceDeletePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "관리자 권한으로 게시글이 삭제되었습니다."));
    }

    @PatchMapping("/{postId}/blind")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminBlindResponse>> blindPost(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody AdminBlindRequest req,
            @LoginAdmin String adminId) {

        AdminBlindResponse response = adminCommunityService.blindPost(postId, req);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 블라인드 처리되었습니다."));
    }

    @RequestMapping(value = "/{postId}/restore", method = {RequestMethod.POST, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminBlindResponse>> restorePost(
            @PathVariable("postId") Long postId,
            @LoginAdmin String adminId) {

        AdminBlindResponse response = adminCommunityService.restorePost(postId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 정상 복구되었습니다."));
    }
}
