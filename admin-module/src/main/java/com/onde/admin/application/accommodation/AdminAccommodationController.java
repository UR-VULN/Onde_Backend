package com.onde.admin.application.accommodation;

import com.onde.admin.application.accommodation.dto.AdminAccommodationStatusRequest;
import com.onde.admin.application.accommodation.dto.AdminAccommodationStatusResponse;
import com.onde.admin.application.accommodation.dto.AdminPendingPropertiesResponse;
import com.onde.admin.application.accommodation.dto.PendingPropertyItem;
import com.onde.admin.application.booking.AdminBookingService;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.PropertyRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accommodations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_ADMIN', 'GENERAL_ADMIN', 'SUPER_ADMIN')")
public class AdminAccommodationController {

    private final AccommodationRepository accommodationRepository;
    private final CarRepository carRepository;
    private final AdminBookingService adminBookingService;
    private final PropertyRepository propertyRepository;

    /**
     * 숙소, 렌터카 전체 예약 내역 조회
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AdminBookingSearchResponse>> searchBookings(
            @ModelAttribute AdminBookingSearchRequest request) {
        
        AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
        return ResponseEntity.ok(ApiResponse.success(response, "예약 내역 조회가 완료되었습니다."));
    }

    /**
     * 대기 매물 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<AdminPendingPropertiesResponse>> getPendingAccommodations(
            @RequestParam(value = "type", required = false) String type) {
        List<PendingPropertyItem> items = new ArrayList<>();
        boolean includeAccommodations = type == null || type.isBlank() || type.equalsIgnoreCase("ACCOMMODATION");
        boolean includeCars = type == null || type.isBlank() || type.equalsIgnoreCase("CAR");

        if (includeAccommodations) {
            accommodationRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
                    .map(accommodation -> new PendingPropertyItem(
                            accommodation.getId(),
                            "ACCOMMODATION",
                            accommodation.getName(),
                            accommodation.getApprovalStatus().name(),
                            accommodation.getSellerId()))
                    .forEach(items::add);
        }
        if (includeCars) {
            carRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
                    .map(car -> new PendingPropertyItem(
                            car.getId(),
                            "CAR",
                            car.getModelName(),
                            car.getApprovalStatus().name(),
                            car.getSellerId()))
                    .forEach(items::add);
        }

        AdminPendingPropertiesResponse response = new AdminPendingPropertiesResponse(items, items.size());
        return ResponseEntity.ok(ApiResponse.success(response, "대기 중인 매물 목록을 조회했습니다."));
    }

    /**
     * 매물 승인/반려
     */
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<AdminAccommodationStatusResponse>> updateAccommodationStatus(
            @PathVariable Long id,
            @RequestBody AdminAccommodationStatusRequest request) {
        
        Accommodation accommodation = accommodationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accommodation not found"));
        
        accommodation.setApprovalStatus(request.approvalStatus());
        accommodationRepository.save(accommodation);

        propertyRepository.findBySellerId(accommodation.getSellerId()).forEach(property -> {
            property.setIsVerified(request.approvalStatus() == ApprovalStatus.APPROVED);
            propertyRepository.save(property);
        });
        
        AdminAccommodationStatusResponse response = new AdminAccommodationStatusResponse(
                accommodation.getId(),
                accommodation.getApprovalStatus(),
                LocalDateTime.now());
        String message = request.approvalStatus() == ApprovalStatus.APPROVED ? "매물이 승인되었습니다." : "매물이 반려되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
