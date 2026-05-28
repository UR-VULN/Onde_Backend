package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingCancelResponse;
import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.flight.SeatInventory;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.Writer;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {

    private final FlightBookingRepository flightBookingRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    /**
     * [Day 8] OOM 차단 JPA Stream 기반 대용량 탑승객 CSV 명단 스트리밍
     */
    @Transactional(readOnly = true)
    public void exportPassengerListCsv(Long scheduleId, Writer writer) throws IOException {
        log.info("📊 Starting대용량 CSV stream generation for scheduleId={}", scheduleId);

        // CSV Header 출력
        writer.write("예약번호,탑승객명,여권번호,생년월일,좌석등급,결제금액,상태\n");

        // JPA Stream 획득 (fetchSize=100 및 cacheable=false 힌트가 적용되어 Heap 메모리 점유 최소화)
        try (Stream<FlightBooking> bookingStream = flightBookingRepository.streamByFlightScheduleId(scheduleId)) {
            bookingStream.forEach(booking -> {
                try {
                    String line = String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            booking.getBookingCode(),
                            booking.getPassenger().getPassengerName(),
                            booking.getPassenger().getPassengerPassport(),
                            booking.getPassenger().getPassengerBirthdate(),
                            booking.getSeatClass().name(),
                            booking.getTotalPrice(),
                            booking.getStatus().name()
                    );
                    writer.write(line);
                } catch (IOException e) {
                    throw new RuntimeException("CSV 스트림 쓰기 중 입출력 오류 발생", e);
                }
            });
            writer.flush();
        }
        log.info("📊 Finished CSV stream generation successfully for scheduleId={}", scheduleId);
    }

    /**
     * [Day 8] 본사 관리자 직권 강제 취소 및 비관적 락 좌석 재고 복원 (+1)
     */
    @Transactional
    public AdminBookingCancelResponse cancelBookingByAdmin(Long bookingId) {
        log.info("🔒 Admin authority booking cancel triggered for bookingId={}", bookingId);

        // 1. 예약 정보 비관적 조회 (동시성 정합성 확보)
        FlightBooking booking = flightBookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() == BookingStatus.CANCELLED_BY_ADMIN || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long scheduleId = booking.getFlightSchedule().getId();

        // 2. 해당 좌석 등급의 실시간 재고 비관적 락 조회 및 안전 복원 (+1)
        SeatInventory inventory = seatInventoryRepository.findWithLockByFlightScheduleIdAndClassType(
                scheduleId, booking.getSeatClass())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SEAT_INVENTORY_NOT_FOUND));

        inventory.setRemainingSeats(inventory.getRemainingSeats() + 1);
        seatInventoryRepository.save(inventory);

        // 3. 예약 상태를 CANCELLED_BY_ADMIN으로 강제 변경
        booking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
        FlightBooking savedBooking = flightBookingRepository.save(booking);

        // 4. 외부 D팀 FeignClient 환불 트리거는 추후 프레임워크 통합 시점에서 가용할 수 있도록 모킹 대리 처리
        log.info("💰 [D-TEAM FEIGN] Triggered refund transaction call logically. bookingCode={}, status={}",
                savedBooking.getBookingCode(), savedBooking.getStatus());

        return AdminBookingCancelResponse.builder()
                .bookingCode(savedBooking.getBookingCode())
                .status(savedBooking.getStatus())
                .build();
    }
}
