package com.onde.admin.application.accommodation;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accommodations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccommodationController {

    private final AccommodationRepository accommodationRepository;

    /**
     * 대기 매물 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Accommodation>>> getPendingAccommodations() {
        List<Accommodation> pendingList = accommodationRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(pendingList, "대기 중인 매물 목록을 조회했습니다."));
    }

    /**
     * 매물 승인/반려
     */
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateAccommodationStatus(
            @PathVariable Long id,
            @RequestParam ApprovalStatus status) {
        
        Accommodation accommodation = accommodationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accommodation not found"));
        
        accommodation.setApprovalStatus(status);
        accommodationRepository.save(accommodation);
        
        String message = status == ApprovalStatus.APPROVED ? "매물이 승인되었습니다." : "매물이 반려되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
}
