package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingCancelResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /**
     * [Day 8] 특정 항공편 탑승객 명단 CSV 대용량 스트림 추출 (OOM 방지 및 UTF-8 BOM 탑재)
     */
    @GetMapping("/api/v1/admin/bookings/flights/{schedule_id}/export")
    public ResponseEntity<StreamingResponseBody> exportPassengerList(
            @PathVariable("schedule_id") Long scheduleId,
            @RequestHeader(value = "X-Admin-Role", required = false) String adminRole) {

        logAdminRole(adminRole);

        StreamingResponseBody responseBody = outputStream -> {
            // Excel 한글 호환을 위해 UTF-8 BOM (\uFEFF) 3바이트를 전두 스트림 출력
            outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                adminBookingService.exportPassengerListCsv(scheduleId, writer);
            } catch (Exception e) {
                log.error("❌ CSV export stream error for scheduleId={}: {}", scheduleId, e.getMessage());
                throw new RuntimeException("CSV 파일 스트리밍 중 오류 발생", e);
            }
        };

        String filename = String.format("passenger_list_schedule_%d.csv", scheduleId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(responseBody);
    }

    /**
     * [Day 8] 관리자 직권 수동 예약 취소 및 비관적 락 좌석 복원 (+1)
     */
    @PostMapping("/api/v1/admin/bookings/{booking_id}/cancel")
    public ResponseEntity<ApiResponse<AdminBookingCancelResponse>> cancelBooking(
            @PathVariable("booking_id") Long bookingId,
            @RequestHeader(value = "X-Admin-Role", required = false) String adminRole) {

        logAdminRole(adminRole);

        AdminBookingCancelResponse response = adminBookingService.cancelBookingByAdmin(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "본사 관리자 직권으로 해당 예약을 수동 강제 취소하고 잔여 좌석을 원복했습니다."));
    }

    private void logAdminRole(String role) {
        if (role != null) {
            log.info("🔑 Admin booking endpoint accessed with role: {}", role);
        }
    }
}
