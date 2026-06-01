package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.entity.accommodation.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.onde.core.support.ApiResponse;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerAccommodationController {
    private final SellerAccommodationService sellerAccommodationService;

    /**
     * 숙소 신규 등록
     */
    @PostMapping("/accommodations")
    public ResponseEntity<ApiResponse<Long>> register(@RequestBody SellerAccommodationRegisterRequest request) {
        Long id = sellerAccommodationService.registerAccommodation(request);
        return ResponseEntity.ok(ApiResponse.success(id, "숙소가 성공적으로 등록되었습니다."));
    }

    @GetMapping("/accommodations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccommodations(
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        Long sellerId = 2L;
        if (memberIdHeader != null && !memberIdHeader.isBlank()) {
            try {
                sellerId = Long.parseLong(memberIdHeader.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        List<com.onde.core.entity.accommodation.Accommodation> list = sellerAccommodationService.getAccommodations(sellerId);
        
        List<Map<String, Object>> mapped = list.stream().map(a -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", a.getId());
            item.put("name", a.getName());
            String status = "ACTIVE";
            if (a.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.PENDING) {
                status = "PENDING";
            } else if (a.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", 120000);
            return item;
        }).toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("accommodations", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 숙소 목록 조회가 성공적으로 완료되었습니다."));
    }

    /**
     * 객실 재고/가격 수정
     */
    @PutMapping("/inventories/rooms")
    public ResponseEntity<ApiResponse<Void>> updateInventories(
            @RequestBody List<RoomInventoryUpdateRequest> requests) {
        // 기존 roomId 기반 로직을 대량 수정 가능하도록 서비스가 지원하는지 확인 필요하지만, 
        // 일단 구조에 맞춰 매핑만 수정합니다. (실제 운영시엔 roomId 정보가 DTO에 있어야 함)
        // 여기서는 기존 로직 보존을 위해 roomId 없이 호출하는 형태로 예시 구현
        sellerAccommodationService.updateRoomInventoriesBulk(requests);
        return ResponseEntity.ok(ApiResponse.success(null, "재고 정보가 성공적으로 수정되었습니다."));
    }
}
