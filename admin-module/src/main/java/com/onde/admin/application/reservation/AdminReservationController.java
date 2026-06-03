package com.onde.admin.application.reservation;

import com.onde.admin.application.booking.AdminBookingService;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.reservation.dto.AdminReservationCancelResponse;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateRequest;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(value = "/{reservationId}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminReservationStatusUpdateResponse>> updateReservationStatus(
            @PathVariable Long reservationId,
            @RequestBody AdminReservationStatusUpdateRequest request) {

        AdminReservationStatusUpdateResponse response =
                adminBookingService.updateReservationStatus(reservationId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "예약 상태가 변경되었습니다."));
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminReservationCancelResponse>> cancelReservation(
            @PathVariable Long reservationId) {

        AdminReservationCancelResponse response = adminBookingService.cancelReservationByAdmin(reservationId);
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 권한으로 예약이 취소되었습니다."));
    }
}
