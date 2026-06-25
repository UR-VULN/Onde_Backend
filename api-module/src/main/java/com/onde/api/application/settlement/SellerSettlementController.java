package com.onde.api.application.settlement;

import com.onde.api.application.settlement.dto.SellerAccountRequest;
import com.onde.api.application.settlement.dto.SellerAccountResponse;
import com.onde.api.application.settlement.dto.SettlementDetailResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SettlementRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 판매자(Seller) 관점에서의 정산 및 정산 계좌 관련 API를 처리하는 통합 컨트롤러 클래스입니다.
 * 스프링 시큐리티 기반의 인가(@PreAuthorize) 제어 하에 작동합니다.
 */
@RestController
@RequestMapping("/api/v1/seller/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerSettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    /**
     * [Day 10] 로그인한 판매자의 정산 내역을 페이징하여 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySettlements(
            @LoginMember Long sellerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {

        Page<Settlement> result = settlementRepository.findBySellerId(sellerId, PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("settlements", result.getContent());
        data.put("totalCount", result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data, "정산 내역 목록 조회가 완료되었습니다."));
    }

    /**
     * [Day 10] 판매자가 보류(PENDING) 상태인 정산 건에 대해 정산금 지급을 본사에 신청(REQUESTED)합니다.
     */
    @PostMapping("/{settlementId}/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestSettlement(
            @PathVariable("settlementId") Long settlementId,
            @LoginMember Long sellerId) {

        Settlement updated = settlementService.requestSettlement(settlementId, sellerId);

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("requestedAt", updated.getRequestedAt());

        return ResponseEntity.ok(ApiResponse.success(data, "정산 신청이 최종 접수되었습니다."));
    }

    /**
     * 판매자가 실시간으로 현재 정산 대상들을 조회하여 직접 즉시 정산을 신청합니다. (정산 ID가 없을 경우 수동 실시간 정산 처리)
     */
    @PostMapping("/request-realtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestSettlementRealTime(
            @LoginMember Long sellerId) {

        Settlement updated = settlementService.requestSettlementRealTime(sellerId);

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("requestedAt", updated.getRequestedAt());

        return ResponseEntity.ok(ApiResponse.success(data, "실시간 정산 신청이 정상적으로 완료되었습니다."));
    }

    /**
     * 판매자 정산 계좌 및 사업자 정보 등록/수정 API (Void 구조로 깔끔하게 최적화)
     */
    @PutMapping("/accounts")
    public ResponseEntity<ApiResponse<SellerAccountResponse>> registerOrUpdateAccount(
            @LoginMember Long sellerId,
            @RequestBody SellerAccountRequest request) {

        SellerAccount account = settlementService.registerOrUpdateAccount(sellerId, request);
        SellerAccountResponse response = toSellerAccountResponse(account);

        return ResponseEntity.ok(ApiResponse.success(response, "정산 계좌가 성공적으로 등록/수정되었습니다."));
    }

    /**
     * 판매자 정산 계좌 및 사업자 정보 조회 API
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<SellerAccountResponse>> getAccount(
            @LoginMember Long sellerId) {

        // 2. 서비스 레이어 메서드 호출 스펙 일치
        Optional<SellerAccount> accountOptional = settlementService.getAccount(sellerId);
        if (accountOptional.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.<SellerAccountResponse>success(
                    null,
                    "등록된 정산 계좌가 없습니다."
            ));
        }

        SellerAccount account = accountOptional.get();

        // 3. 응답 DTO 조립 및 마스킹 처리 가두리화
        SellerAccountResponse response = toSellerAccountResponse(account);

        return ResponseEntity.ok(ApiResponse.success(response, "정산 계좌 정보를 성공적으로 조회했습니다."));
    }

    private SellerAccountResponse toSellerAccountResponse(SellerAccount account) {
        return SellerAccountResponse.builder()
                .sellerId(account.getMember().getId())
                .bankName(account.getBankName())
                .businessName(account.getBusinessName())
                .contactPhone(com.onde.core.util.DataMaskingUtil.maskPhoneNumber(account.getContactPhone()))
                .businessAddress(account.getBusinessAddress())
                .accountNumber(settlementService.maskAccountNumber(account.getAccountNumber()))
                .accountHolder(account.getAccountHolder())
                .businessNumber(com.onde.core.util.DataMaskingUtil.maskBusinessNumber(account.getBusinessNumber()))
                .representativeName(com.onde.core.util.DataMaskingUtil.maskName(account.getRepresentativeName()))
                .openedAt(com.onde.core.util.DataMaskingUtil.maskDate(account.getOpenedAt()))
                .createdAt(account.getCreatedAt())
                .build();
    }

    /**
     * [Day 10 추가] 특정 정산 건의 상세 예약 결제 내역들을 조회합니다.
     */
    @GetMapping("/{settlementId}/details")
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlementDetails(
            @PathVariable("settlementId") Long settlementId,
            @LoginMember Long sellerId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));

        if (!settlement.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("본인의 정산 내역만 조회할 수 있습니다.");
        }

        List<PaymentRepository.SettlementDetailProjection> projections =
                settlementService.getSettlementDetails(settlementId, sellerId);

        List<SettlementDetailResponse.DetailItem> items = projections.stream()
                .map(p -> SettlementDetailResponse.DetailItem.builder()
                        .paymentId(p.getPaymentId())
                        .reservationId(p.getReservationId())
                        .targetType(p.getTargetType())
                        .productName(p.getProductName())
                        .amount(p.getAmount())
                        .paymentDate(p.getPaymentDate())
                        .build())
                .collect(Collectors.toList());

        SettlementDetailResponse response = SettlementDetailResponse.builder()
                .settlementId(settlement.getId())
                .settlementDate(settlement.getSettlementDate())
                .details(items)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "정산 상세 내역 조회가 완료되었습니다."));
    }
}
