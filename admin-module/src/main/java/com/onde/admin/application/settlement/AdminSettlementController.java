package com.onde.admin.application.settlement;

import com.onde.admin.application.settlement.dto.AdminSettlementDetailResponse;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SettlementRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * [플랫폼 본사 관리자(Admin) 관점 정산 처리 컨트롤러]
 * 각 입점 파트너사(판매자)가 요청한 매출 정산 데이터를 검토, 승인 및 지급 처리하는 API입니다.
 * - 정산 내역 필터링 및 페이징 조회
 * - 1차 정산 담당자(SELLER_ADMIN)의 1차 승인 (APPROVED_1ST)
 * - 본사 최고 관리자(SUPER_ADMIN)의 최종 승인 및 지급 확정 (COMPLETED)
 */
@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;
    private final SettlementRepository settlementRepository;

    /**
     * [전체 판매자 대상 정산 내역 페이징 조회 API]
     * 본사 정산 담당자가 등록된 전체 정산 데이터를 확인하는 데 사용됩니다.
     * 특정 정산 상태(예: REQUESTED, APPROVED_1ST 등)로 필터링하여 페이징 조회가 가능합니다.
     *
     * @param status   필터링할 정산 상태 (선택 사항, 미입력 시 전체 상태 조회)
     * @param page     조회할 페이지 번호 (0부터 시작)
     * @param size     한 페이지당 출력할 정산 데이터 수
     * @return 페이징된 정산 데이터 목록, 입점사 계좌 정보 및 전체 엘리먼트 개수를 담은 성공 공통 응답 객체
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettlements(
            @RequestParam(name = "status", required = false) SettlementStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        Page<Settlement> result;
        // 1. 상태(status) 파라미터 유무에 따라 조건별 분기 조회 수행
        if (status != null) {
            result = settlementRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            result = settlementRepository.findAll(PageRequest.of(page, size));
        }
        
        java.util.List<Map<String, Object>> settlementList = new java.util.ArrayList<>();
        for (Settlement s : result.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("settlementId", s.getId());
            // 정산일자 문자열 변환 매핑
            map.put("settlementMonth", s.getSettlementDate().toString());
            // 매출 원금액 (판매 총액)
            map.put("grossAmount", s.getGrossAmount());
            // 플랫폼 수수료액
            map.put("commission", s.getCommission());
            // 판매자에게 지급할 실정산 금액 (grossAmount - commission)
            map.put("netAmount", s.getNetAmount());
            // 정산 진행 단계 상태
            map.put("status", s.getStatus());
            
            // 2. 판매자(SellerId) 정보를 바탕으로 입금받을 금융 계좌 정보 매핑 및 결합
            // (명세 요구사항에 따라 sellerName, bankName, accountNumber를 추가 반환함)
            com.onde.core.entity.settlement.SellerAccount account = adminSettlementService.getAccount(s.getSellerId());
            map.put("sellerName", account.getAccountHolder()); // 대표 계좌 예금주명을 sellerName 대용으로 매핑
            map.put("bankName", account.getBankName());
            // 계좌 정보 오남용 방지를 위해 일부 마스킹 처리하여 반환
            map.put("accountNumber", adminSettlementService.maskAccountNumber(account.getAccountNumber()));
            
            settlementList.add(map);
        }

        // 최종 응답 DTO 맵 구성
        Map<String, Object> data = new HashMap<>();
        data.put("settlements", settlementList);
        data.put("totalCount", result.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * [1차 정산 담당자(SELLER_ADMIN)의 정산 1차 승인 API]
     * 판매자가 신청(REQUESTED)한 정산 건을 검토한 뒤 1차 승인 상태(APPROVED_1ST)로 전이시킵니다.
     *
     * @param settlementId 1차 승인을 가동할 정산 테이블 식별자 ID
     * @param body         1차 담당자가 남기는 추가 심사 의견(comment) 정보
     * @return 1차 승인이 완료된 정산 식별 ID 및 갱신 상태 정보
     */
    @PostMapping("/{settlementId}/approve-first")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveFirst(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody Map<String, String> body) {
            
        // 비즈니스 서비스 레이어로 넘겨 1차 승인 로직 격리 수행
        Settlement updated = adminSettlementService.approveFirstSettlement(settlementId, body.get("comment"));

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("approvedAt", updated.getApprovedAt());
        
        return ResponseEntity.ok(ApiResponse.success(data, "1차 승인 처리되었습니다."));
    }

    @PostMapping("/{settlementId}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody(required = false) Map<String, String> body) {
        Settlement updated = adminSettlementService.approveSettlement(
                settlementId,
                body != null ? body.get("comment") : null);

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("approvedAt", updated.getApprovedAt());

        return ResponseEntity.ok(ApiResponse.success(data, "정산 승인 완료"));
    }

    @PostMapping("/{settlementId}/reject")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody(required = false) Map<String, String> body) {
        String rejectReason = body != null ? body.getOrDefault("rejectReason", body.get("comment")) : null;
        LocalDateTime rejectedAt = LocalDateTime.now();
        Settlement updated = adminSettlementService.rejectSettlement(
                settlementId,
                rejectReason);

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("rejectReason", rejectReason);
        data.put("rejectedAt", rejectedAt);

        return ResponseEntity.ok(ApiResponse.success(data, "정산이 반려되었습니다."));
    }

    /**
     * [본사 최고 관리자(SUPER_ADMIN)의 최종 정산 확정 및 지급 완료 API]
     * 1차 승인(APPROVED_1ST)을 거친 정산 건에 대해 입금을 확정 짓고 정산 절차를 완료(COMPLETED) 상태로 종결합니다.
     *
     * @param settlementId 최종 지급 처리를 완료할 정산 테이블 식별자 ID
     * @param body         최종 승인 검토 의견(comment) 정보
     * @return 최종 지급 및 확정이 완결된 정산 식별 ID 및 상태 정보
     */
    @PostMapping("/{settlementId}/finalize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeSettlement(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody Map<String, String> body) {
            
        // 비즈니스 서비스 레이어로 넘겨 2차/최종 정산 완료 처리 수행
        Settlement updated = adminSettlementService.finalizeSettlement(settlementId, body.get("comment"));

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("finalizedAt", updated.getFinalizedAt());
        
        return ResponseEntity.ok(ApiResponse.success(data, "정산이 최종 확정되었습니다."));
    }

    /**
     * [본사 관리자 대상 정산 상세 내역 조회 API]
     */
    @GetMapping("/{settlementId}/details")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminSettlementDetailResponse>> getSettlementDetails(
            @PathVariable("settlementId") Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건이 존재하지 않습니다."));

        List<PaymentRepository.SettlementDetailProjection> projections =
                adminSettlementService.getSettlementDetails(settlementId);

        List<AdminSettlementDetailResponse.DetailItem> items = projections.stream()
                .map(p -> AdminSettlementDetailResponse.DetailItem.builder()
                        .paymentId(p.getPaymentId())
                        .reservationId(p.getReservationId())
                        .targetType(p.getTargetType())
                        .productName(p.getProductName())
                        .amount(p.getAmount())
                        .paymentDate(p.getPaymentDate())
                        .build())
                .collect(Collectors.toList());

        AdminSettlementDetailResponse response = AdminSettlementDetailResponse.builder()
                .settlementId(settlement.getId())
                .settlementDate(settlement.getSettlementDate())
                .details(items)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "정산 상세 내역 조회가 완료되었습니다."));
    }
}
