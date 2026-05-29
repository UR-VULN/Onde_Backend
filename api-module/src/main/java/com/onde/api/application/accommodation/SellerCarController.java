package com.onde.api.application.accommodation;

import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerCarController {

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
