package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    // 1. 예약 이용 완료 강제 처리 API (아까 작성한 부분)
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, String>> forceCompleteBooking(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> request) {
        
        adminBookingService.forceCompleteBooking(bookingId);
        return ResponseEntity.ok(Map.of("message", "예약 ID " + bookingId + "번의 이용 상태가 완료로 강제 업데이트 되었습니다."));
    }

    // 2. 예약 내역 검색 API 
    @GetMapping("/search")
    public ResponseEntity<AdminBookingSearchResponse> searchBookings(
            @ModelAttribute AdminBookingSearchRequest request) {
        
        // 서비스의 검색 로직 호출
        AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
        return ResponseEntity.ok(response);
    }
}