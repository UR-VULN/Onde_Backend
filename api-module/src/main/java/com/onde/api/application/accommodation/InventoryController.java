package com.onde.api.application.accommodation;

import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/inventory", "/api/inventory"})
@RequiredArgsConstructor
public class InventoryController {
    private final ReservationService reservationService;
    private final InventoryRepository inventoryRepository;

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

    //비로그인 및 일반사용자용 특정 상품의 월별 재고 및 가격 달력 데이터를 조회합니다.
    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getCalendar(
            @RequestParam ReservationTarget targetType,
            @RequestParam Long targetId,
            @RequestParam String month) {

        YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Inventory> dbInventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                targetType, targetId, startDate, endDate);

        Map<LocalDate, Inventory> dbMap = new HashMap<>();
        for (Inventory inv : dbInventories) {
            dbMap.put(inv.getDate(), inv);
        }

        Map<String, Map<String, Object>> response = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            Inventory inv = dbMap.get(date);
            String dayKey = String.valueOf(d);

            Map<String, Object> dayInfo = new HashMap<>();
            if (inv != null) {
                boolean isClosed = inv.getStock() == null || inv.getStock() <= 0;
                dayInfo.put("stock", inv.getStock() != null ? inv.getStock() : 0);
                dayInfo.put("price", inv.getBasePrice() != null ? inv.getBasePrice().longValue() : 0L);
                dayInfo.put("isClosed", isClosed);
            } else {
                dayInfo.put("stock", 0);
                dayInfo.put("price", 0L);
                dayInfo.put("isClosed", true);
            }
            response.put(dayKey, dayInfo);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "재고 달력 조회가 완료되었습니다.",
            "data", response
        ));
    }
}

