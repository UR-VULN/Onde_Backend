package com.onde.api.application.flight;

import com.onde.api.application.flight.dto.FlightBookingRequest;
import com.onde.api.application.flight.dto.FlightBookingResponse;
import com.onde.api.application.flight.dto.FlightSearchRequest;
import com.onde.api.application.flight.dto.FlightSearchResponse;
import com.onde.api.config.DistributedLockExecutor;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final DistributedLockExecutor distributedLockExecutor;

    @GetMapping("/flights/search")
    public ResponseEntity<ApiResponse<FlightSearchResponse>> searchFlights(@ModelAttribute FlightSearchRequest req) {
        FlightSearchResponse response = flightService.searchFlights(req);
        return ResponseEntity.ok(ApiResponse.success(response, "항공편 검색 성공"));
    }

    /**
     * Redisson 분산 락 실행기(DistributedLockExecutor)를 활용한 동시성 방어 예약 진입
     */
    @PostMapping("/reservations/flights")
    public ResponseEntity<ApiResponse<FlightBookingResponse>> bookSeat(
            @Valid @RequestBody FlightBookingRequest req,
            @LoginMember Long userId) {
        String lockKey = "flight:lock:" + req.getScheduleId() + ":" + req.getSeatClass().name();

        // Redisson 분산 락 획득 시도 (대기 5초, 점유 10초) 및 비즈니스 콜백 격리 트랜잭션 실행
        FlightBookingResponse response = distributedLockExecutor.executeWithLock(lockKey, 5, 10, () -> {
            return flightService.bookSeat(req, userId);
        });

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "선택하신 좌석이 분산 락 제어 하에 10분간 안전하게 선점되었습니다."));
    }

    /**
     * SAGA 패턴 기반 보상 트랜잭션 (외부 결제 성공 후 로컬 DB 갱신 실패 시 자동 승인 취소 결합)
     */
    @PostMapping("/reservations/flights/{booking_code}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @PathVariable("booking_code") String bookingCode,
            @RequestBody Map<String, Object> paymentPayload) {
        log.info("💳 Payment confirmation request received for bookingCode: {}", bookingCode);
        String pgTransactionId = (String) paymentPayload.getOrDefault("pgTransactionId", "PG-TX-DEFAULT-12345");
        BigDecimal amount = new BigDecimal(paymentPayload.getOrDefault("paymentAmount", "0").toString());

        // 1. 외부 결제 승인 가상 성공 (Log 기록)
        log.info("💰 [PAYMENT SUCCESS] Payment approved. pgTransactionId={}, amount={}", pgTransactionId, amount);

        // 2. 로컬 DB 데이터 갱신 시도 (시뮬레이션 예외 처리)
        try {
            // 강제 실패 힌트("FAIL")가 들어오는 경우 로컬 DB 정합성 장애 상황 모의 유발
            if (pgTransactionId.contains("FAIL")) {
                throw new org.springframework.dao.DataRetrievalFailureException(
                        "Database connection timed out during reservation status update.");
            }

            // [정상 흐름]: 실제 비즈니스에서는 예약 상태를 CONFIRMED로 최종 갱신
            flightService.confirmBooking(bookingCode);
            log.info("🎉 [DB SUCCESS] FlightBooking status updated to CONFIRMED for bookingCode={}", bookingCode);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("bookingCode", bookingCode, "status", "CONFIRMED", "pgTransactionId", pgTransactionId),
                    "결제 승인 및 예약 확정이 최종 완료되었습니다."));
        } catch (Exception e) {
            log.error("❌ [DB ERROR] Local database update failed due to: {}", e.getMessage());

            // 3. [보상 트랜잭션 집행 (SAGA Pattern Compensation)]
            // 로컬 DB 장애 감지 시, 승인되었던 외부 결제 승인을 비동기/동기로 즉각 전액 강제 취소 요청
            log.warn("🔄 [SAGA COMPENSATING] 로컬 DB 갱신 장애 감지! 승인된 결제건에 대해 즉시 자동 취소를 청구합니다. targetPgTxId={}",
                    pgTransactionId);

            // 환불 FeignClient 연동 시뮬레이션 및 가상 결제 취소 연동
            triggerCompensatingRefund(pgTransactionId, amount);

            // 최종 비즈니스 예외 전파
            throw new com.onde.core.exception.ValidationException(
                    com.onde.core.exception.ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void triggerCompensatingRefund(String pgTransactionId, BigDecimal amount) {
        log.info("💰 [SAGA COMPENSATION MOCK SUCCESS] Auto-refund mock completed for pgTransactionId={} with amount={}", pgTransactionId, amount);
    }
}
