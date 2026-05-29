package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingCancelResponse;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/bookings") // 👈 v1 규격 및 도메인 베이스 패스 단일화
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_ADMIN', 'SUPER_ADMIN')") // 👈 본사 관리자 권한 이중 잠금
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /**
     * [첫 번째 코드 스펙] 예약 내역 검색 (숙소/렌터카 도메인)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AdminBookingSearchResponse>> searchBookings(
            @ModelAttribute AdminBookingSearchRequest request) {
        
        AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
        return ResponseEntity.ok(ApiResponse.success(response, "예약 내역 조회가 완료되었습니다."));
    }

    /**
     * [첫 번째 코드 Skinner] 예약 이용 완료 강제 처리 (숙소/렌터카 도메인)
     */
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<Void>> forceCompleteBooking(
            @PathVariable("bookingId") Long bookingId) {
        
        adminBookingService.forceCompleteBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "해당 예약의 이용 상태가 완료로 강제 업데이트되었습니다."));
    }

    /**
     * [두 번째 코드 스펙] 특정 항공편 탑승객 명단 CSV 대용량 스트림 추출 (OOM 방지 및 UTF-8 BOM 탑재)
     */
    @GetMapping("/flights/{scheduleId}/export") // 👈 카멜케이스 변수명 정돈 및 중복 패스 정리
    public ResponseEntity<StreamingResponseBody> exportPassengerList(
            @PathVariable("scheduleId") Long scheduleId) {

        log.info("🔑 Admin booking endpoint accessed for exporting passenger list: {}", scheduleId);

        StreamingResponseBody responseBody = outputStream -> {
            // Excel 한글 깨짐 방지를 위해 UTF-8 BOM (\uFEFF) 3바이트를 전두 스트림 출력
            outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                adminBookingService.exportPassengerListCsv(scheduleId, writer);
            } catch (Exception e) {
                log.error("❌ CSV export stream error for scheduleId={}: {}", scheduleId, e.getMessage());
                throw new RuntimeException("CSV 파일 스트리밍 중 오류 발생", e);
            }
        };

        String filename = String.format("passenger_list_schedule_%d.csv", scheduleId);

        // 파일 다운로드 바이너리 응답 스트림은 ApiResponse로 감싸지 않고 본래의 순정 껍데기를 유지해야 브라우저가 파싱합니다.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(responseBody);
    }

    /**
     * [두 번째 코드 스펙] 관리자 직권 수동 예약 취소 및 비관적 락 좌석 복원 (+1) (항공 도메인)
     */
    @PostMapping("/{bookingId}/cancel") // 👈 베이스 패스 중복 정리 및 변수명 카멜케이스 전환
    public ResponseEntity<ApiResponse<AdminBookingCancelResponse>> cancelBooking(
            @PathVariable("bookingId") Long bookingId) {

        log.info("🔑 Admin booking endpoint accessed for cancelling booking: {}", bookingId);

        AdminBookingCancelResponse response = adminBookingService.cancelBookingByAdmin(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "본사 관리자 직권으로 해당 예약을 수동 강제 취소하고 잔여 좌석을 원복했습니다."));
    }
}