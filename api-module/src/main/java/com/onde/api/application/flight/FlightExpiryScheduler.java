package com.onde.api.application.flight;

import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.flight.SeatInventory;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightExpiryScheduler {

    private final FlightBookingRepository flightBookingRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    /**
     * [Day 3] 10분 이내 결제가 완료되지 않은 임시 예약 건에 대해 매 1분 주기로 자동 좌석 재고 복구(+1) 실행
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void recoverExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<FlightBooking> expiredBookings = flightBookingRepository.findByStatusAndReservedUntilBefore(
                BookingStatus.PENDING_PAYMENT, now);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("⏰ Found {} expired flight bookings. Starting auto-recovery process...", expiredBookings.size());

        for (FlightBooking booking : expiredBookings) {
            try {
                Long scheduleId = booking.getFlightSchedule().getId();

                // 1. 해당 좌석 등급의 실시간 재고 1 증가 원복
                SeatInventory inventory = seatInventoryRepository.findWithLockByFlightScheduleIdAndClassType(
                        scheduleId, booking.getSeatClass())
                        .orElse(null);

                if (inventory != null) {
                    inventory.setRemainingSeats(inventory.getRemainingSeats() + 1);
                    seatInventoryRepository.save(inventory);
                }

                // 2. 예약 상태를 시간초과취소(CANCELLED_BY_TIMEOUT)로 변경
                booking.setStatus(BookingStatus.CANCELLED_BY_TIMEOUT);
                flightBookingRepository.save(booking);

                log.info("⏰ [SUCCESS] Auto-recovered expired seat: bookingCode={}, scheduleId={}, classType={}",
                        booking.getBookingCode(), scheduleId, booking.getSeatClass());
            } catch (Exception e) {
                log.error("⏰ [ERROR] Failed to recover bookingCode={}: {}", booking.getBookingCode(), e.getMessage());
            }
        }
    }
}
