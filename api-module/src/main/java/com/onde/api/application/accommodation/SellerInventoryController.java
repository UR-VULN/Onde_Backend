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

/**
 * 판매자용 숙소/렌터카 일별 재고 및 가격 제어를 담당하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/seller/inventory")
@RequiredArgsConstructor
public class SellerInventoryController {

    private final InventoryRepository inventoryRepository;

    /**
     * 특정 상품(숙소 객실 또는 차량)의 월별 재고 및 가격 달력 데이터를 조회합니다.
     *
     * @param propertyKey 매물 키 (예: stay-1, car-2 등)
     * @param monthStr 조회 연월 (예: 2026-05)
     * @return 1일부터 마지막 날까지의 일별 재고, 가격, 예약 마감 상태 정보
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Map<String, CalendarDayInfo>>> getCalendar(
            @RequestParam("propertyKey") String propertyKey,
            @RequestParam("month") String monthStr) {

        // propertyKey를 파싱하여 대상 타입(ROOM/CAR)과 식별자(ID) 추출 (예: stay-1 -> ROOM, 1)
        ReservationTarget targetType = parseTargetType(propertyKey);
        Long targetId = parseTargetId(propertyKey);

        // 조회할 대상 월 파싱 및 시작일/종료일 계산 (예: 2026-05)
        YearMonth ym = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 데이터베이스에서 해당 기간의 일별 재고 데이터를 일괄 조회
        List<Inventory> dbInventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                targetType, targetId, startDate, endDate);

        Map<LocalDate, Inventory> dbMap = new HashMap<>();
        for (Inventory inv : dbInventories) {
            dbMap.put(inv.getDate(), inv);
        }

        // 해당 월의 모든 일자에 대해 응답 맵 구성 (데이터가 없거나 재고가 0인 경우 Closed 처리)
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

    /**
     * 특정 상품의 하루치 재고 및 가격 설정을 업데이트하거나 새로 생성합니다.
     *
     * @param request 달력 업데이트 정보 DTO
     * @return 성공 여부
     */
    @PatchMapping("/calendar")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateCalendar(@RequestBody CalendarUpdateRequest request) {
        ReservationTarget targetType = parseTargetType(request.getPropertyKey());
        Long targetId = parseTargetId(request.getPropertyKey());

        // 대상 연월 설정 (기본값: "2026-05")
        String monthStr = request.getMonth() != null ? request.getMonth() : "2026-05";
        YearMonth ym = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate date = ym.atDay(request.getDay());

        // 기존 재고 엔티티가 존재하면 가져오고, 없으면 신규 생성
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

        // 요청 데이터로 재고 및 가격 정보 갱신
        if (request.getStock() != null) {
            inventory.setStock(request.getStock());
        }
        if (request.getPrice() != null) {
            inventory.setBasePrice(BigDecimal.valueOf(request.getPrice()));
        }

        inventoryRepository.save(inventory);

        return ResponseEntity.ok(ApiResponse.success(null, "달력 재고 및 금액이 성공적으로 변경되었습니다."));
    }

    /**
     * propertyKey의 접두사를 기반으로 예약 대상 타입(ROOM/CAR)을 판별합니다.
     */
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

    /**
     * propertyKey에서 하이픈 뒤의 ID 값을 파싱하여 가져옵니다. (예: "stay-15" -> 15L)
     */
    private Long parseTargetId(String propertyKey) {
        if (propertyKey == null || !propertyKey.contains("-")) {
            throw new IllegalArgumentException("Invalid propertyKey format");
        }
        String[] parts = propertyKey.split("-");
        return Long.parseLong(parts[1]);
    }

    /**
     * 일별 재고/가격 상세 데이터 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarDayInfo {
        private Integer stock;      // 잔여 재고 수량
        private Long price;         // 해당 일자 기본 요금
        private Boolean isClosed;   // 예약 마감(Close) 여부
    }

    /**
     * 달력 정보 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarUpdateRequest {
        private String propertyKey; // 예: stay-1
        private String month;       // 예: "2026-05" (미지정 시 기본 2026-05로 바인딩)
        private Integer day;        // 일자 (1 ~ 31)
        private Integer stock;      // 변경할 재고 수량
        private Long price;         // 변경할 가격
    }
}
