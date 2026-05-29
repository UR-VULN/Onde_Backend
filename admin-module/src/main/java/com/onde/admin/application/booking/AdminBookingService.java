package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingCancelResponse;
import com.onde.admin.application.booking.dto.AdminBookingDto;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.flight.SeatInventory;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {

    private final ReservationRepository reservationRepository;
    private final FlightBookingRepository flightBookingRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    /**
     * 투숙객/이용자 명단 조회 (숙소/렌터카 도메인)
     */
    public AdminBookingSearchResponse searchBookings(AdminBookingSearchRequest request) {
        List<Reservation> reservations = reservationRepository.findAll();
        
        List<AdminBookingDto> dtos = reservations.stream()
                .map(r -> new AdminBookingDto(
                        r.getId(),
                        "예약자명", 
                        "숙소/렌터카명", 
                        null, 
                        null, 
                        r.getStatus()
                ))
                .collect(Collectors.toList());

        return new AdminBookingSearchResponse(dtos, dtos.size());
    }

    /**
     * 이용 완료 상태 강제 업데이트 (숙소/렌터카 도메인)
     */
    @Transactional
    public void forceCompleteBooking(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INTERNAL_SERVER_ERROR));

        reservation.setStatus(ReservationStatus.COMPLETED); 
    }

    /**
     * 대용량 탑승객 CSV 명단 스트리밍 (항공 도메인)
     */
    public void exportPassengerListCsv(Long scheduleId, Writer writer) throws IOException {
        log.info("📊 Starting CSV stream generation for scheduleId={}", scheduleId);

        writer.write("예약번호,탑승객명,여권번호,생년월일,좌석등급,결제금액,상태\n");

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
     * 본사 관리자 직권 강제 취소 및 비관적 락 좌석 재고 복원 (항공 도메인)
     */
    @Transactional
    public AdminBookingCancelResponse cancelBookingByAdmin(Long bookingId) {
        log.info("🔒 Admin authority booking cancel triggered for bookingId={}", bookingId);

        FlightBooking booking = flightBookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() == BookingStatus.CANCELLED_BY_ADMIN || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long scheduleId = booking.getFlightSchedule().getId();

        SeatInventory inventory = seatInventoryRepository.findWithLockByFlightScheduleIdAndClassType(
                scheduleId, booking.getSeatClass())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SEAT_INVENTORY_NOT_FOUND));

        inventory.setRemainingSeats(inventory.getRemainingSeats() + 1);
        seatInventoryRepository.save(inventory);

        booking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
        FlightBooking savedBooking = flightBookingRepository.save(booking);

        log.info("💰 [D-TEAM FEIGN] Triggered refund transaction call logically. bookingCode={}, status={}",
                savedBooking.getBookingCode(), savedBooking.getStatus());

        return AdminBookingCancelResponse.builder()
                .bookingCode(savedBooking.getBookingCode())
                .status(savedBooking.getStatus())
                .build();
    }
}