package com.onde.api.application.flight;

import com.onde.api.application.flight.dto.SellerCalendarResponse;
import com.onde.api.application.flight.dto.SellerFlightRegisterRequest;
import com.onde.api.application.flight.dto.SellerFlightRegisterResponse;
import com.onde.api.application.flight.dto.SellerScheduleControlRequest;
import com.onde.api.application.flight.dto.SellerScheduleControlResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SellerFlightController {

    private final SellerFlightService sellerFlightService;

    @PostMapping("/api/v1/seller/flights")
    public ResponseEntity<ApiResponse<SellerFlightRegisterResponse>> registerBulkSchedules(
            @RequestBody SellerFlightRegisterRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        
        Long actualSellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                actualSellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // Ignore and fallback
            }
        }

        SellerFlightRegisterResponse response = sellerFlightService.registerBulkSchedules(req, actualSellerId);
        String message = String.format("총 %d개의 스케줄이 정상적으로 생성되어 본사 검수 대기 상태에 등록되었습니다.", response.getCreatedCount());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, message));
    }

    @GetMapping("/api/v1/seller/flights/calendar")
    public ResponseEntity<ApiResponse<List<SellerCalendarResponse>>> getCalendarSchedules(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {

        Long actualSellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                actualSellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        List<SellerCalendarResponse> response = sellerFlightService.getCalendarSchedules(year, month, actualSellerId);
        return ResponseEntity.ok(ApiResponse.success(response, "월별 스케줄 및 실시간 잔여석 목록을 성공적으로 조회했습니다."));
    }

    @PatchMapping({"/api/v1/seller/schedules/{schedule_id}/control", "/api/v1/seller/flights/schedules/{schedule_id}/control"}) // 다중 엔드포인트 오타 완전 수용
    public ResponseEntity<ApiResponse<SellerScheduleControlResponse>> controlSchedule(
            @PathVariable("schedule_id") Long scheduleId,
            @RequestBody SellerScheduleControlRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {

        Long actualSellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                actualSellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        SellerScheduleControlResponse response = sellerFlightService.controlSchedule(scheduleId, req, actualSellerId);
        return ResponseEntity.ok(ApiResponse.success(response, "선택하신 항공편의 실시간 재고 및 가격 조절을 완료했습니다."));
    }
}
