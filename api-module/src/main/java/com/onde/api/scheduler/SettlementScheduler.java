package com.onde.api.scheduler;

import com.onde.api.application.settlement.SettlementService;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 정산 업무의 백그라운드 배치를 주기적으로 호출하는 스케줄러 컴포넌트입니다.
 */
@Slf4j
@Component
@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementService settlementService;

    /**
     * 매일 자정(0시 0분 0초)에 전날(어제) 하루 동안의 전체 결제(PAID) 거래액을 집계하여 정산 데이터를 생성합니다.
     * 크론 표현식: "0 0 0 * * ?" -> 초 분 시 일 월 요일 (매일 00:00:00)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailySettlement() {
        // 1. 현재 일자 기준 전일(어제) 날짜 구하기
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("일간 정산 배치 스케줄러가 시작되었습니다. (대상 일자: {})", yesterday);

        try {
            // 2. 일간 정산 로직 실행
            settlementService.executeDailySettlement(yesterday);
            log.info("일간 정산 배치가 성공적으로 처리되었습니다. (대상 일자: {})", yesterday);
        } catch (Exception e) {
            log.error("일간 정산 배치 실행 중 오류가 발생했습니다. (대상 일자: {})", yesterday, e);
        }
    }

    /**
     * [테스트/관리자 전용] 특정 날짜의 정산 배치를 수동으로 즉시 실행합니다.
     * @param date 집계 대상 날짜 (예: 2026-06-04), 미입력 시 어제 날짜로 실행
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALES_ADMIN', 'SELLER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSettlement(
            @RequestParam(name = "date", required = false) String date) {

        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now().minusDays(1);

        log.info("[수동 트리거] 정산 배치 강제 실행 요청. (대상 일자: {})", targetDate);

        try {
            settlementService.executeDailySettlement(targetDate);
            log.info("[수동 트리거] 정산 배치 완료. (대상 일자: {})", targetDate);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("targetDate", targetDate.toString(), "status", "SUCCESS"),
                    targetDate + " 정산 배치가 성공적으로 실행되었습니다."
            ));
        } catch (Exception e) {
            log.error("[수동 트리거] 정산 배치 실행 중 오류. (대상 일자: {})", targetDate, e);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("targetDate", targetDate.toString(), "status", "ERROR", "message", e.getMessage()),
                    "정산 배치 실행 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
