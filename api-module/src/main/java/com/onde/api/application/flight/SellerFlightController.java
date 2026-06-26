package com.onde.api.application.flight;

import com.onde.api.application.flight.dto.SellerCalendarResponse;
import com.onde.api.application.flight.dto.SellerFlightRegisterRequest;
import com.onde.api.application.flight.dto.SellerFlightRegisterResponse;
import com.onde.api.application.flight.dto.SellerScheduleControlRequest;
import com.onde.api.application.flight.dto.SellerScheduleControlResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
public class SellerFlightController {

    private final SellerFlightService sellerFlightService;

    @PostMapping("/api/v1/seller/flights")
    public ResponseEntity<ApiResponse<SellerFlightRegisterResponse>> registerBulkSchedules(
            @Valid @RequestBody SellerFlightRegisterRequest req,
            @LoginMember Long actualSellerId) {
        
        SellerFlightRegisterResponse response = sellerFlightService.registerBulkSchedules(req, actualSellerId);
        String message = String.format("총 %d개의 스케줄이 정상적으로 생성되어 본사 검수 대기 상태에 등록되었습니다.", response.getCreatedCount());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, message));
    }

    @GetMapping({"/api/v1/seller/schedules/calendar", "/api/v1/seller/flights/calendar"})
    public ResponseEntity<ApiResponse<List<SellerCalendarResponse>>> getCalendarSchedules(
            @RequestParam(value = "routeId", required = false) @Min(1) Long routeId,
            @RequestParam("year") @Min(2000) @Max(2100) Integer year,
            @RequestParam("month") @Min(1) @Max(12) Integer month,
            @LoginMember Long actualSellerId) {

        List<SellerCalendarResponse> response = sellerFlightService.getCalendarSchedules(routeId, year, month, actualSellerId);
        return ResponseEntity.ok(ApiResponse.success(response, "월별 스케줄 및 실시간 잔여석 목록을 성공적으로 조회했습니다."));
    }

    @PatchMapping({"/api/v1/seller/schedules/{scheduleId}/control", "/api/v1/seller/flights/schedules/{scheduleId}/control"}) // 다중 엔드포인트 오타 완전 수용
    public ResponseEntity<ApiResponse<SellerScheduleControlResponse>> controlSchedule(
            @PathVariable("scheduleId") @Min(1) Long scheduleId,
            @Valid @RequestBody SellerScheduleControlRequest req,
            @LoginMember Long actualSellerId) {

        SellerScheduleControlResponse response = sellerFlightService.controlSchedule(scheduleId, req, actualSellerId);
        return ResponseEntity.ok(ApiResponse.success(response, "선택하신 항공편의 실시간 재고 및 가격 조절을 완료했습니다."));
    }

    @GetMapping("/api/v1/seller/flights")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFlights(
            @LoginMember Long actualSellerId) {
        List<com.onde.core.entity.flight.FlightRoute> list = sellerFlightService.getRoutesBySellerId(actualSellerId);
        List<Map<String, Object>> mapped = list.stream().map(route -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", route.getId());
            item.put("name", String.format("%s ➡️ %s (%d분)", 
                    route.getDepartureAirport(), 
                    route.getArrivalAirport(), 
                    route.getDurationMinutes()));
            
            String status = "ACTIVE";
            if (route.getStatus() == com.onde.core.entity.flight.ApprovalStatus.PENDING_APPROVAL) {
                status = "PENDING";
            } else if (route.getStatus() == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", 120000);
            return item;
        }).toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("flights", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 항공 노선 목록 조회가 성공적으로 완료되었습니다."));
    }
}
