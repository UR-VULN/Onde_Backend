package com.onde.api.application.accommodation;

import com.onde.core.entity.reservation.ReservationTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/inventory", "/api/inventory"})
@RequiredArgsConstructor
public class InventoryController {
    private final ReservationService reservationService;

    /**
     * 비동기 재고 상태 조회 API
     * 특정 기간 내에 해당 상품이 품절(재고 부족)인지 실시간으로 확인합니다.
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam ReservationTarget targetType,
            @RequestParam Long targetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        boolean isAvailable = reservationService.checkAvailability(targetType, targetId, startDate, endDate);
        
        return ResponseEntity.ok(Map.of(
            "targetId", targetId,
            "isAvailable", isAvailable,
            "status", isAvailable ? "AVAILABLE" : "SOLD_OUT"
        ));
    }
}
