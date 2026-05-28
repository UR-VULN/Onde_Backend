package com.onde.api.application.settlement;

import com.onde.api.application.settlement.SettlementService;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
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
 * 플랫폼 본사 관리자(Admin) 관점에서의 정산 검토 및 지급을 처리하는 컨트롤러 클래스입니다.
 * 정산 건 조회, 1차 승인(APPROVED_1ST), 최종 확정 및 지급 완료(COMPLETED) 상태 처리를 지원합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    /**
     * 관리자가 전체 판매자의 정산 내역을 조회합니다. 특정 상태(status) 필터를 걸어 조회할 수 있습니다.
     *
     * @param status   필터링할 정산 상태 (선택 사항, 지정하지 않으면 전체 조회)
     * @param page     조회할 페이지 번호 (0부터 시작)
     * @param size     한 페이지당 출력할 정산 데이터 수
     * @return 페이징된 정산 목록 및 총 개수를 포함한 공통 응답 객체
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettlements(
            @RequestParam(name = "status", required = false) SettlementStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        Page<Settlement> result;
        if (status != null) {
            result = settlementRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            result = settlementRepository.findAll(PageRequest.of(page, size));
        }
        
        java.util.List<Map<String, Object>> settlementList = new java.util.ArrayList<>();
        for (Settlement s : result.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("settlementId", s.getId());
            map.put("settlementMonth", s.getSettlementDate().toString());
            map.put("grossAmount", s.getGrossAmount());
            map.put("commission", s.getCommission());
            map.put("netAmount", s.getNetAmount());
            map.put("status", s.getStatus());
            
            // 판매자 계좌 정보 함께 결합 반환 (명세 요구사항: sellerName, bankName, accountNumber)
            com.onde.core.entity.settlement.SellerAccount account = settlementService.getAccount(s.getSellerId());
            map.put("sellerName", account.getAccountHolder()); // 대표자명을 sellerName 대용 혹은 별도 지정
            map.put("bankName", account.getBankName());
            map.put("accountNumber", settlementService.maskAccountNumber(account.getAccountNumber()));
            
            settlementList.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("settlements", settlementList);
        data.put("totalCount", result.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 1차 정산 담당자(SALES_ADMIN)가 판매자의 지급 신청(REQUESTED) 건을 확인 후 승인(APPROVED_1ST) 처리합니다.
     *
     * @param settlementId 1차 승인할 정산 건의 식별자
     * @param body         요청 바디에 담긴 의견(comment) 정보
     * @return 1차 승인이 완료된 정산 정보
     */
    @PostMapping("/{settlementId}/approve-first")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveFirst(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody Map<String, String> body) {
            
        Settlement updated = settlementService.approveFirstSettlement(settlementId, body.get("comment"));

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("approvedAt", updated.getApprovedAt());
        
        return ResponseEntity.ok(ApiResponse.success(data, "1차 승인 처리되었습니다."));
    }

    /**
     * 본사 최고 관리자(SUPER_ADMIN)가 1차 승인(APPROVED_1ST) 완료된 정산 건을 최종 승인 및 지급 완료(COMPLETED) 처리합니다.
     *
     * @param settlementId 최종 확정할 정산 건의 식별자
     * @param body         요청 바디에 담긴 의견(comment) 정보
     * @return 최종 확정 완료된 정산 정보
     */
    @PostMapping("/{settlementId}/finalize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeSettlement(
            @PathVariable("settlementId") Long settlementId,
            @RequestBody Map<String, String> body) {
            
        Settlement updated = settlementService.finalizeSettlement(settlementId, body.get("comment"));

        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("finalizedAt", updated.getFinalizedAt());
        
        return ResponseEntity.ok(ApiResponse.success(data, "정산이 최종 확정되었습니다."));
    }
}


