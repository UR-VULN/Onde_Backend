package com.onde.admin.application.booking;

import com.onde.admin.application.booking.dto.AdminBookingCancelResponse;
import com.onde.admin.application.booking.dto.AdminBookingDto;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.admin.application.booking.dto.AdminBookingStatusUpdateRequest;
import com.onde.admin.application.booking.dto.AdminBookingStatusUpdateResponse;
import com.onde.admin.application.reservation.dto.AdminReservationCancelResponse;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateRequest;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateResponse;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private final com.onde.core.repository.MemberRepository memberRepository;
    private final com.onde.core.repository.RoomRepository roomRepository;
    private final com.onde.core.repository.CarRepository carRepository;
    private final com.onde.core.repository.FlightScheduleRepository flightScheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 투숙객/이용자 명단 조회 (숙소/렌터카 도메인)
     */
    public AdminBookingSearchResponse searchBookings(AdminBookingSearchRequest request) {
        List<Reservation> reservations = reservationRepository.findAll();
        
        List<AdminBookingDto> dtos = reservations.stream()
                .filter(r -> request.status() == null || r.getStatus() == request.status())
                .filter(r -> matchesTargetType(r, request.targetType()))
                .filter(r -> request.startDate() == null || !r.getCheckIn().toLocalDate().isBefore(request.startDate()))
                .filter(r -> request.endDate() == null || !r.getCheckOut().toLocalDate().isAfter(request.endDate()))
                .map(r -> new AdminBookingDto(
                        r.getId(),
                        resolveMemberName(r.getUserId()),
                        resolveTargetName(r),
                        r.getCheckIn(),
                        r.getCheckOut(),
                        r.getStatus()
                ))
                .filter(dto -> request.memberName() == null || dto.memberName().contains(request.memberName()))
                .collect(Collectors.toList());

        return new AdminBookingSearchResponse(dtos, dtos.size());
    }

    private boolean matchesTargetType(Reservation reservation, String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return true;
        }
        String normalized = targetType.trim().toUpperCase();
        return switch (normalized) {
            case "ACCOMMODATION", "ROOM", "STAYS" -> reservation.getTargetType() == com.onde.core.entity.reservation.ReservationTarget.ROOM;
            case "CAR", "CARS", "RENTAL_CAR" -> reservation.getTargetType() == com.onde.core.entity.reservation.ReservationTarget.CAR;
            default -> true;
        };
    }

    private String resolveMemberName(Long userId) {
        return memberRepository.findById(userId)
                .map(member -> member.getName() != null ? member.getName() : member.getEmail())
                .orElse("알 수 없음");
    }

    private String resolveTargetName(Reservation reservation) {
        if (reservation.getTargetType() == com.onde.core.entity.reservation.ReservationTarget.ROOM) {
            return roomRepository.findById(reservation.getTargetId())
                    .map(room -> room.getAccommodation().getName())
                    .orElse("숙소");
        }
        if (reservation.getTargetType() == com.onde.core.entity.reservation.ReservationTarget.CAR) {
            return carRepository.findById(reservation.getTargetId())
                    .map(com.onde.core.entity.accommodation.Car::getModelName)
                    .orElse("렌터카");
        }
        return "예약 상품";
    }

    /**
     * 이용 완료 상태 강제 업데이트 (숙소/렌터카 도메인)
     */
    @Transactional
    public void forceCompleteBooking(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        reservation.setStatus(ReservationStatus.COMPLETED); 
    }

    /**
     * 관리자 공통 예약 상태 수동 변경 (숙소/렌터카 도메인)
     */
    @Transactional
    public AdminReservationStatusUpdateResponse updateReservationStatus(
            Long reservationId,
            AdminReservationStatusUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        ReservationStatus previousStatus = reservation.getStatus();
        ReservationStatus nextStatus = ReservationStatus.valueOf(request.status().trim().toUpperCase());
        reservation.setStatus(nextStatus);
        Reservation savedReservation = reservationRepository.save(reservation);

        return new AdminReservationStatusUpdateResponse(
                savedReservation.getId(),
                previousStatus,
                savedReservation.getStatus(),
                savedReservation.getUpdatedAt()
        );
    }

    /**
     * 관리자 공통 예약 직권 취소 (숙소/렌터카 도메인)
     */
    @Transactional
    public AdminReservationCancelResponse cancelReservationByAdmin(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation savedReservation = reservationRepository.save(reservation);
        return new AdminReservationCancelResponse(savedReservation.getId(), savedReservation.getStatus());
    }

    /**
     * 관리자 항공 예약 상태 수동 변경
     */
    @Transactional
    public AdminBookingStatusUpdateResponse updateFlightBookingStatus(Long bookingId, AdminBookingStatusUpdateRequest request) {
        FlightBooking booking = flightBookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOKING_NOT_FOUND));

        BookingStatus previousStatus = booking.getStatus();
        BookingStatus nextStatus = BookingStatus.valueOf(request.status().trim().toUpperCase());
        booking.setStatus(nextStatus);

        FlightBooking savedBooking = flightBookingRepository.save(booking);
        return new AdminBookingStatusUpdateResponse(
                savedBooking.getId(),
                savedBooking.getBookingCode(),
                previousStatus,
                savedBooking.getStatus(),
                savedBooking.getUpdatedAt()
        );
    }

    // [보안 강화 - ADMIN-14] CSV 수식 주입 공격 (Excel Formula Injection) 방어를 위해 이스케이프 적용
    public String sanitizeCsvCell(Object value) {
        if (value == null) return "";
        String str = String.valueOf(value);
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@")) {
            return "\t" + str; // 탭 문자를 삽입하여 엑셀 실행 수식 트리거 차단
        }
        return str;
    }

    /**
     * 대용량 탑승객 CSV 명단 스트리밍 (항공 도메인)
     */
    public void exportPassengerListCsv(Long scheduleId, Long requesterId, Writer writer) throws IOException {
        log.info("📊 Starting CSV stream generation for scheduleId={} by requesterId={}", scheduleId, requesterId);

        // 1. 스케줄 정보 조회
        com.onde.core.entity.flight.FlightSchedule schedule = flightScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

        // 2. [보안 강화 - ADMIN-3 / IDOR 방어] 최고 관리자(SUPER_ADMIN)가 아닌 경우, 스케줄의 노선 소유자(sellerId) 교차 검증 강제
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin && schedule.getRoute() != null && !schedule.getRoute().getSellerId().equals(requesterId)) {
            throw new com.onde.core.exception.ForbiddenException(com.onde.core.exception.ErrorCode.FORBIDDEN);
        }

        writer.write("예약번호,탑승객명,여권번호,생년월일,좌석등급,결제금액,상태\n");

        try (Stream<FlightBooking> bookingStream = flightBookingRepository.streamByFlightScheduleId(scheduleId)) {
            bookingStream.forEach(booking -> {
                try {
                    // [보안 강화 - ADMIN-14] CSV Injection 방어를 위해 각 데이터에 sanitizeCsvCell() 적용
                    String line = String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            sanitizeCsvCell(booking.getBookingCode()),
                            sanitizeCsvCell(booking.getPassenger().getPassengerName()),
                            sanitizeCsvCell(booking.getPassenger().getPassengerPassport()),
                            sanitizeCsvCell(booking.getPassenger().getPassengerBirthdate()),
                            sanitizeCsvCell(booking.getSeatClass().name()),
                            sanitizeCsvCell(booking.getTotalPrice()),
                            sanitizeCsvCell(booking.getStatus().name())
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

        log.info("💰 Publishing AdminBookingCancelEvent for payment refund and mileage restore. bookingId={}", bookingId);
        
        // 이벤트 발행을 통해 결제 취소 및 마일리지 복구 로직 비동기 연동
        eventPublisher.publishEvent(new com.onde.core.event.AdminBookingCancelEvent(this, bookingId, savedBooking.getUserId(), "FLIGHT"));

        return AdminBookingCancelResponse.builder()
                .bookingCode(savedBooking.getBookingCode())
                .status(savedBooking.getStatus())
                .build();
    }
}
