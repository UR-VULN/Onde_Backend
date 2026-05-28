package com.onde.api.application.settlement;

import com.onde.api.application.settlement.SettlementService;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.repository.SettlementRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 판매자(Seller) 관점에서의 정산 관련 API를 처리하는 컨트롤러 클래스입니다.
 * 판매자가 본인의 정산 내역을 확인하거나 정산금을 신청(REQUESTED)하는 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/seller/settlements")
@RequiredArgsConstructor
public class SellerSettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    /**
     * 로그인한 판매자의 정산 내역을 페이징하여 조회합니다.
     *
     * @param sellerId 판매자 식별자 (임시로 X-Seller-Id 헤더에서 추출)
     * @param page     조회할 페이지 번호 (0부터 시작)
     * @param size     한 페이지당 출력할 정산 데이터 수
     * @return 정산 목록 및 총 개수가 담긴 맵 형태의 공통 응답 객체
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySettlements(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {

        Page<Settlement> result = settlementRepository.findBySellerId(sellerId, PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("settlements", result.getContent());
        data.put("totalCount", result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 판매자가 보류(PENDING) 상태인 정산 건에 대해 정산금 지급을 본사에 신청(REQUESTED)합니다.
     *
     * @param settlementId 신청할 정산 데이터의 식별자 (PK)
     * @param sellerId     지급 신청을 요청한 판매자 식별자
     * @return 상태가 REQUESTED로 업데이트된 정산 식별자 및 상태 응답 객체
     */
    @PostMapping("/{settlementId}/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestSettlement(
            @PathVariable("settlementId") Long settlementId,
            @RequestHeader("X-Seller-Id") Long sellerId) {

        Settlement updated = settlementService.requestSettlement(settlementId, sellerId);

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("requestedAt", updated.getRequestedAt());

        return ResponseEntity.ok(ApiResponse.success(data, "정산 신청이 접수되었습니다."));
    }

    /**
     * 정산 계좌 등록/수정 API
     */
    @PutMapping("/accounts")
    public ResponseEntity<ApiResponse<com.onde.api.application.settlement.dto.SellerAccountResponse>> registerOrUpdateAccount(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestBody com.onde.api.application.settlement.dto.SellerAccountRequest request) {

        com.onde.core.entity.settlement.SellerAccount account = settlementService.registerOrUpdateAccount(sellerId, request);
        
        com.onde.api.application.settlement.dto.SellerAccountResponse response = com.onde.api.application.settlement.dto.SellerAccountResponse.builder()
                .sellerId(account.getSellerId())
                .bankName(account.getBankName())
                .accountNumber(settlementService.maskAccountNumber(account.getAccountNumber()))
                .accountHolder(account.getAccountHolder())
                .businessNumber(account.getBusinessNumber())
                .representativeName(account.getRepresentativeName())
                .openedAt(account.getOpenedAt())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "정산 계좌가 등록/수정되었습니다."));
    }

    /**
     * 정산 계좌 조회 API
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<com.onde.api.application.settlement.dto.SellerAccountResponse>> getAccount(
            @RequestHeader("X-Seller-Id") Long sellerId) {

        com.onde.core.entity.settlement.SellerAccount account = settlementService.getAccount(sellerId);

        com.onde.api.application.settlement.dto.SellerAccountResponse response = com.onde.api.application.settlement.dto.SellerAccountResponse.builder()
                .sellerId(account.getSellerId())
                .bankName(account.getBankName())
                .accountNumber(settlementService.maskAccountNumber(account.getAccountNumber()))
                .accountHolder(account.getAccountHolder())
                .businessNumber(account.getBusinessNumber())
                .representativeName(account.getRepresentativeName())
                .openedAt(account.getOpenedAt())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "정산 계좌 조회 성공."));
    }
}
