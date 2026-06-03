package com.onde.admin.application.reservation;

import com.onde.admin.application.booking.AdminBookingService;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER_ADMIN', 'USER_ADMIN', 'SUPER_ADMIN')")
public class AdminReservationController {

    private final AdminBookingService adminBookingService;

    /**
     * 전체 예약 내역 검색
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminBookingSearchResponse>> getAllReservations(
            @ModelAttribute AdminBookingSearchRequest request) {
        
        // AdminBookingService의 searchBookings가 이미 전체 검색 로직을 일부 포함하고 있음
        AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
        return ResponseEntity.ok(ApiResponse.success(response, "전체 예약 내역 조회가 완료되었습니다."));
    }
}
