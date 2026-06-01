package com.onde.api.application.accommodation;

import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerCarController {

    private final CarService carService;

    @GetMapping("/cars")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCars(
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        Long sellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                sellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        List<com.onde.core.entity.accommodation.Car> list = carService.getCarsBySellerId(sellerId);
        List<Map<String, Object>> mapped = list.stream().map(c -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", c.getId());
            item.put("name", c.getModelName());
            String status = "ACTIVE";
            if (c.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.PENDING) {
                status = "PENDING";
            } else if (c.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", 50000);
            return item;
        }).toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("cars", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 렌터카 목록 조회가 성공적으로 완료되었습니다."));
    }

    /**
     * 렌터카 신규 등록
     */
    @PostMapping("/cars")
    public ResponseEntity<ApiResponse<Long>> registerCar(@RequestBody Map<String, Object> request) {
        // TODO: 비즈니스 로직 연동
        return ResponseEntity.ok(ApiResponse.success(1L, "렌터카 등록 신청이 완료되었습니다."));
    }

    /**
     * 렌터카 재고/가격 수정
     */
    @PutMapping("/inventories/cars")
    public ResponseEntity<ApiResponse<Void>> updateCarInventory(@RequestBody Map<String, Object> request) {
        // TODO: 비즈니스 로직 연동
        return ResponseEntity.ok(ApiResponse.success(null, "렌터카 재고 및 가격이 수정되었습니다."));
    }
}
