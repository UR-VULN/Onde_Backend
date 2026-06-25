package com.onde.api.application.flight;

import com.onde.api.application.flight.dto.FlightBookingRequest;
import com.onde.api.application.flight.dto.FlightBookingResponse;
import com.onde.api.application.flight.dto.FlightSearchRequest;
import com.onde.api.application.flight.dto.FlightSearchResponse;
import com.onde.core.entity.flight.*;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.FlightScheduleRepository;
import com.onde.core.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final FlightBookingRepository flightBookingRepository;

    @Cacheable(value = "flightSearch", key = "#a0", unless = "#result == null")
    public FlightSearchResponse searchFlights(FlightSearchRequest req) {
        log.info("✈️ Real-time flight search triggered with query parameters: {}", req);

        List<String> deps = req.getDepartureList();
        List<String> arrs = req.getArrivalList();
        List<LocalDate> dates = req.getDateList();
        SeatClass targetClass = req.getParsedSeatClass();
        Integer passengers = req.getPassengerCount() != null ? req.getPassengerCount() : 1;

        int journeyCount = dates.size();
        List<FlightSearchResponse.ScheduleDto> resultSchedules = new ArrayList<>();

        for (int i = 0; i < journeyCount; i++) {
            String dep = deps.get(Math.min(i, deps.size() - 1));
            String arr = arrs.get(Math.min(i, arrs.size() - 1));
            LocalDate date = dates.get(i);

            LocalDateTime startTime = date.atStartOfDay();
            LocalDateTime endTime = date.atTime(LocalTime.MAX);

            List<FlightSchedule> schedules = flightScheduleRepository.findApprovedSchedules(dep, arr, startTime, endTime);

            if (!schedules.isEmpty()) {
                List<Long> scheduleIds = schedules.stream()
                        .map(FlightSchedule::getId)
                        .collect(Collectors.toList());

                List<SeatInventory> inventories = seatInventoryRepository.findByFlightScheduleIdIn(scheduleIds);

                Map<Long, List<SeatInventory>> inventoryMap = inventories.stream()
                        .collect(Collectors.groupingBy(SeatInventory::getFlightScheduleId));

                for (FlightSchedule fs : schedules) {
                    List<SeatInventory> fsInventories = inventoryMap.getOrDefault(fs.getId(), Collections.emptyList());

                    fsInventories.stream()
                            .filter(si -> targetClass == null || si.getClassType() == targetClass)
                            .filter(si -> si.getRemainingSeats() >= passengers)
                            .map(si -> FlightSearchResponse.ScheduleDto.builder()
                                    .scheduleId(fs.getId())
                                    .flightNumber(fs.getFlightNumber())
                                    .origin(fs.getRoute().getDepartureAirport())
                                    .destination(fs.getRoute().getArrivalAirport())
                                    .departureTime(fs.getDepartureTime())
                                    .arrivalTime(fs.getArrivalTime())
                                    .durationMinutes(fs.getRoute().getDurationMinutes())
                                    .seatClass(si.getClassType())
                                    .remainingSeats(si.getRemainingSeats())
                                    .basePrice(si.getBasePrice())
                                    .build())
                            .forEach(resultSchedules::add);
                }
            }
        }

        return FlightSearchResponse.builder()
                .schedules(resultSchedules)
                .totalCount(resultSchedules.size())
                .build();
    }

    /**
     * [Day 3] 실시간 좌석 임시 선점 예약 (비관적 락 동시성 제어 적용)
     */
    @Transactional
    public FlightBookingResponse bookSeat(FlightBookingRequest req, Long userId) {
        log.info("🔒 Attempting to hold seat for scheduleId={}, classType={}, userId={}", req.getScheduleId(), req.getSeatClass(), userId);

        // 1. 항공 스케줄 존재 여부 확인
        FlightSchedule schedule = flightScheduleRepository.findById(req.getScheduleId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

        // 2. 비관적 쓰기 락(PESSIMISTIC_WRITE)을 획득하여 실시간 재고 조회 (데드락 방지 및 오버부킹 원천 격리)
        SeatInventory inventory = seatInventoryRepository.findWithLockByFlightScheduleIdAndClassType(req.getScheduleId(), req.getSeatClass())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SEAT_INVENTORY_NOT_FOUND));

        // 3. 잔여석 및 가격 검증
        int passengerCount = Math.max(1, req.passengerCount());
        if (inventory.getRemainingSeats() < passengerCount) {
            log.warn("⚠️ Seat sold out for scheduleId={}, classType={}", req.getScheduleId(), req.getSeatClass());
            throw new ValidationException(ErrorCode.SEAT_SOLD_OUT);
        }

        java.math.BigDecimal expectedPrice = inventory.getBasePrice().multiply(java.math.BigDecimal.valueOf(passengerCount));
        if (req.getTotalPrice() != null && req.getTotalPrice().compareTo(expectedPrice) != 0) {
            log.warn("⚠️ Price manipulation detected! Expected: {}, Received: {}", expectedPrice, req.getTotalPrice());
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 4. 잔여석 1 감소 차감
        inventory.setRemainingSeats(inventory.getRemainingSeats() - passengerCount);
        seatInventoryRepository.save(inventory);

        // 5. 고유 예약 코드 생성 (30자 이하 규격 준수)
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String bookingCode = "BK-" + todayStr + "-" + randomSuffix;

        // 6. 예약 객체 생성 및 영속화 (10분 선점 기한 바인딩)
        Passenger passenger = Passenger.builder()
                .passengerName(req.getPassengerName())
                .passengerPassport(req.getPassengerPassport())
                .passengerBirthdate(req.getPassengerBirthdate())
                .build();

        FlightBooking booking = FlightBooking.builder()
                .bookingCode(bookingCode)
                .flightSchedule(schedule)
                .userId(userId)
                .passenger(passenger)
                .seatClass(req.getSeatClass())
                .totalPrice(expectedPrice)
                .status(BookingStatus.PENDING_PAYMENT)
                .reservedUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        FlightBooking savedBooking = flightBookingRepository.save(booking);
        log.info("🎉 Held seat successfully. bookingCode={}, reservedUntil={}", savedBooking.getBookingCode(), savedBooking.getReservedUntil());

        return FlightBookingResponse.builder()
                .bookingId(savedBooking.getId())
                .bookingCode(savedBooking.getBookingCode())
                .scheduleId(savedBooking.getFlightSchedule().getId())
                .flightNumber(savedBooking.getFlightSchedule().getFlightNumber())
                .passengerName(savedBooking.getPassenger().getPassengerName())
                .seatClass(savedBooking.getSeatClass())
                .totalPrice(savedBooking.getTotalPrice())
                .status(savedBooking.getStatus())
                .reservedUntil(savedBooking.getReservedUntil())
                .build();
    }

    /**
     * [Day 9] 예약 결제 확정 로직 (예약 상태를 CONFIRMED로 최종 업데이트)
     */
    @Transactional
    public void confirmBooking(String bookingCode, java.math.BigDecimal paymentAmount) {
        FlightBooking booking = flightBookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND)); // 예외 코드는 기존 정의된 NotFoundException 활용

        if (paymentAmount != null && booking.getTotalPrice().compareTo(paymentAmount) != 0) {
            log.warn("⚠️ Payment amount mismatch! Expected: {}, Received: {}", booking.getTotalPrice(), paymentAmount);
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        flightBookingRepository.save(booking);
    }
}
