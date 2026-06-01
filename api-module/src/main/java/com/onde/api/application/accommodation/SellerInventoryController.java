package com.onde.api.application.accommodation;

import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.support.ApiResponse;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/seller/inventory")
@RequiredArgsConstructor
public class SellerInventoryController {

    private final InventoryRepository inventoryRepository;

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Map<String, CalendarDayInfo>>> getCalendar(
            @RequestParam("propertyKey") String propertyKey,
            @RequestParam("month") String monthStr) {

        // Parse propertyKey (e.g. stay-1 -> ROOM, 1)
        ReservationTarget targetType = parseTargetType(propertyKey);
        Long targetId = parseTargetId(propertyKey);

        // Parse month (e.g. 2026-05)
        YearMonth ym = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // Get from DB
        List<Inventory> dbInventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                targetType, targetId, startDate, endDate);

        Map<LocalDate, Inventory> dbMap = new HashMap<>();
        for (Inventory inv : dbInventories) {
            dbMap.put(inv.getDate(), inv);
        }

        // Construct response for every day of the month
        Map<String, CalendarDayInfo> response = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            Inventory inv = dbMap.get(date);
            String dayKey = String.valueOf(d);

            if (inv != null) {
                boolean isClosed = inv.getStock() == null || inv.getStock() <= 0;
                response.put(dayKey, CalendarDayInfo.builder()
                        .stock(inv.getStock() != null ? inv.getStock() : 0)
                        .price(inv.getBasePrice() != null ? inv.getBasePrice().longValue() : 0L)
                        .isClosed(isClosed)
                        .build());
            } else {
                response.put(dayKey, CalendarDayInfo.builder()
                        .stock(0)
                        .price(0L)
                        .isClosed(true)
                        .build());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(response, "재고 달력 조회가 완료되었습니다."));
    }

    @PatchMapping("/calendar")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateCalendar(@RequestBody CalendarUpdateRequest request) {
        ReservationTarget targetType = parseTargetType(request.getPropertyKey());
        Long targetId = parseTargetId(request.getPropertyKey());

        // Default year/month to 2026-05 if not specified (since example month in prompt is 2026-05)
        String monthStr = request.getMonth() != null ? request.getMonth() : "2026-05";
        YearMonth ym = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate date = ym.atDay(request.getDay());

        Optional<Inventory> opt = inventoryRepository.findByTargetTypeAndTargetIdAndDate(targetType, targetId, date);
        Inventory inventory;
        if (opt.isPresent()) {
            inventory = opt.get();
        } else {
            inventory = new Inventory();
            inventory.setTargetType(targetType);
            inventory.setTargetId(targetId);
            inventory.setDate(date);
        }

        if (request.getStock() != null) {
            inventory.setStock(request.getStock());
        }
        if (request.getPrice() != null) {
            inventory.setBasePrice(BigDecimal.valueOf(request.getPrice()));
        }

        inventoryRepository.save(inventory);

        return ResponseEntity.ok(ApiResponse.success(null, "달력 재고 및 금액이 성공적으로 변경되었습니다."));
    }

    private ReservationTarget parseTargetType(String propertyKey) {
        if (propertyKey == null) {
            throw new IllegalArgumentException("propertyKey cannot be null");
        }
        String lower = propertyKey.toLowerCase();
        if (lower.startsWith("stay") || lower.startsWith("room")) {
            return ReservationTarget.ROOM;
        } else if (lower.startsWith("car")) {
            return ReservationTarget.CAR;
        }
        throw new IllegalArgumentException("Unknown propertyKey prefix: " + propertyKey);
    }

    private Long parseTargetId(String propertyKey) {
        if (propertyKey == null || !propertyKey.contains("-")) {
            throw new IllegalArgumentException("Invalid propertyKey format");
        }
        String[] parts = propertyKey.split("-");
        return Long.parseLong(parts[1]);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarDayInfo {
        private Integer stock;
        private Long price;
        private Boolean isClosed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarUpdateRequest {
        private String propertyKey;
        private String month; // e.g. "2026-05" (optional)
        private Integer day;
        private Integer stock;
        private Long price;
    }
}
