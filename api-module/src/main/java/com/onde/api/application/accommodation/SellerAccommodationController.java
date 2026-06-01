package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.entity.accommodation.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerAccommodationController {
    private final SellerAccommodationService sellerAccommodationService;

    // 판매자 등록 숙소 신규 등록 API (주소 규격 보정 포함)

    @PostMapping("/accommodations")
    public ResponseEntity<ApiResponse<Long>> register(@RequestBody SellerAccommodationRegisterRequest request) {
        Long id = sellerAccommodationService.registerAccommodation(request);
        return ResponseEntity.ok(ApiResponse.success(id, "숙소가 성공적으로 등록되었습니다."));
    }

    // 판매자 등록 숙소 목록 조회 API
    @GetMapping("/accommodations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccommodations(
            @LoginMember Long sellerId) {
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

    // 객실 재고/가격 수정 (특정 객실 대상)
    @PutMapping("/accommodations/rooms/{roomId}/inventory")
    public ResponseEntity<ApiResponse<Void>> updateRoomInventory(
            @PathVariable Long roomId,
            @RequestBody List<@Valid RoomInventoryUpdateRequest> requests) {
        sellerAccommodationService.updateRoomInventories(roomId, requests);
        return ResponseEntity.ok(ApiResponse.success(null, "객실 재고 정보가 성공적으로 수정되었습니다."));
    }

    // 객실 재고/가격 수정 (벌크)
    @PutMapping("/inventories/rooms")
    public ResponseEntity<ApiResponse<Void>> updateInventories(
            @RequestBody List<@Valid RoomInventoryUpdateRequest> requests) {
        sellerAccommodationService.updateRoomInventoriesBulk(requests);
        return ResponseEntity.ok(ApiResponse.success(null, "재고 정보가 성공적으로 수정되었습니다."));
    }
}
