package com.onde.api.scheduler;

import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 결제 완료되지 않고 10분이 지난 숙소 및 렌터카 예약을 매 1분마다 자동 취소(soft-release) 처리하고 실재고를 원복합니다.
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(10);
        
        // 결제 대기(RESERVED) 상태로 10분이 초과한 예약 목록 조회
        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndCreatedAtBefore(ReservationStatus.RESERVED, limitTime);

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("⏰ Found {} expired accommodation/car reservations. Releasing inventory...", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            try {
                // 1. 예약 상태 취소 변경
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservationRepository.save(reservation);

                // 2. 재고 반환 복원
                LocalDate startDate = reservation.getCheckIn().toLocalDate();
                LocalDate endDate = reservation.getCheckOut().toLocalDate().minusDays(1);

                List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                        reservation.getTargetType(), reservation.getTargetId(), startDate, endDate);

                for (Inventory inventory : inventories) {
                    inventory.setStock(inventory.getStock() + 1);
                    inventoryRepository.save(inventory);
                }

                log.info("⏰ [SUCCESS] Auto-released reservation: id={}, targetType={}, targetId={}",
                        reservation.getId(), reservation.getTargetType(), reservation.getTargetId());
            } catch (Exception e) {
                log.error("⏰ [ERROR] Failed to auto-release reservation id={}: {}", reservation.getId(), e.getMessage());
            }
        }
    }
}
