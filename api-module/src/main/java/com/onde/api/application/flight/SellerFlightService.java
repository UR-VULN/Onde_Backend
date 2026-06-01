package com.onde.api.application.flight;

import com.onde.api.application.flight.dto.SellerCalendarResponse;
import com.onde.api.application.flight.dto.SellerFlightRegisterRequest;
import com.onde.api.application.flight.dto.SellerFlightRegisterResponse;
import com.onde.api.application.flight.dto.SellerScheduleControlRequest;
import com.onde.api.application.flight.dto.SellerScheduleControlResponse;
import com.onde.core.entity.flight.*;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.FlightRouteRepository;
import com.onde.core.repository.FlightScheduleRepository;
import com.onde.core.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SellerFlightService {

    private final FlightRouteRepository flightRouteRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final FlightBookingRepository flightBookingRepository;

    /**
     * [Day 5] 판매자 항공 스케줄 및 좌석 재고 일괄 배치(Bulk) 등록
     */
    @Transactional
    public SellerFlightRegisterResponse registerBulkSchedules(SellerFlightRegisterRequest req, Long sellerId) {
        log.info("✈️ Bulk flight schedules generation triggered by sellerId={}, routeId={}", sellerId, req.getRouteId());

        FlightRoute route = flightRouteRepository.findById(req.getRouteId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT_VALUE));

        LocalDate current = req.getStartDate();
        LocalDate end = req.getEndDate();
        List<Integer> targetDays = req.getOperatingDays();

        int createdCount = 0;

        while (!current.isAfter(end)) {
            int dayOfWeekValue = current.getDayOfWeek().getValue();
            if (targetDays.contains(dayOfWeekValue)) {
                LocalDateTime departureTime = current.atTime(req.getDepartureTime());
                LocalDateTime arrivalTime = departureTime.plusMinutes(req.getDurationMinutes());

                FlightSchedule schedule = FlightSchedule.builder()
                        .route(route)
                        .flightNumber(req.getFlightNumber())
                        .departureTime(departureTime)
                        .arrivalTime(arrivalTime)
                        .status(ApprovalStatus.PENDING_APPROVAL)
                        .build();

                FlightSchedule savedSchedule = flightScheduleRepository.save(schedule);

                for (SellerFlightRegisterRequest.SeatSetupDto seat : req.getSeatSetup()) {
                    SeatInventory inventory = SeatInventory.builder()
                            .flightScheduleId(savedSchedule.getId())
                            .classType(seat.getClassType())
                            .totalSeats(seat.getTotalSeats())
                            .remainingSeats(seat.getTotalSeats())
                            .basePrice(seat.getBasePrice())
                            .build();

                    seatInventoryRepository.save(inventory);
                }

                createdCount++;
            }
            current = current.plusDays(1);
        }

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String batchGroupId = "BG-" + todayStr + "-" + randomSuffix;

        log.info("✈️ Bulk generation completed. batchGroupId={}, createdCount={}", batchGroupId, createdCount);

        return SellerFlightRegisterResponse.builder()
                .batchGroupId(batchGroupId)
                .createdCount(createdCount)
                .status("PENDING_APPROVAL")
                .build();
    }

    /**
     * [Day 6] 백오피스 달력 UI용 스케줄 및 실시간 잔여 좌석 조회
     */
    public List<SellerCalendarResponse> getCalendarSchedules(Integer year, Integer month, Long sellerId) {
        log.info("📅 Loading calendar schedules for sellerId={}, year={}, month={}", sellerId, year, month);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1. 해당 월 전체 항공 스케줄 조회
        List<FlightSchedule> schedules = flightScheduleRepository.findByDepartureTimeBetween(
                start.atStartOfDay(), end.atTime(LocalTime.MAX));

        if (schedules.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> scheduleIds = schedules.stream()
                .map(FlightSchedule::getId)
                .collect(Collectors.toList());

        // 2. 스케줄별 좌석 정보 일괄 조회
        List<SeatInventory> inventories = seatInventoryRepository.findByFlightScheduleIdIn(scheduleIds);
        Map<Long, List<SeatInventory>> inventoryMap = inventories.stream()
                .collect(Collectors.groupingBy(SeatInventory::getFlightScheduleId));

        List<SellerCalendarResponse> responses = new ArrayList<>();
        for (FlightSchedule fs : schedules) {
            List<SeatInventory> fsInventories = inventoryMap.getOrDefault(fs.getId(), Collections.emptyList());

            for (SeatInventory si : fsInventories) {
                responses.add(SellerCalendarResponse.builder()
                        .scheduleId(fs.getId())
                        .flightNumber(fs.getFlightNumber())
                        .departureAirport(fs.getRoute().getDepartureAirport())
                        .arrivalAirport(fs.getRoute().getArrivalAirport())
                        .departureTime(fs.getDepartureTime())
                        .classType(si.getClassType())
                        .totalSeats(si.getTotalSeats())
                        .remainingSeats(si.getRemainingSeats())
                        .basePrice(si.getBasePrice())
                        .status(fs.getStatus())
                        .build());
            }
        }

        return responses;
    }

    /**
     * [Day 6] 달력 기반 요금 및 실시간 재고 수동 제어 (Active 예약 대조 검증)
     */
    @Transactional
    public SellerScheduleControlResponse controlSchedule(Long scheduleId, SellerScheduleControlRequest req, Long sellerId) {
        log.info("🔒 Controlling scheduleId={} by sellerId={}, type={}", scheduleId, sellerId, req.getControlType());

        // 1. 스케줄 유효성 검증
        FlightSchedule schedule = flightScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

        // 2. 대상 등급의 재고 비관적 락 조회
        SeatInventory inventory = seatInventoryRepository.findWithLockByFlightScheduleIdAndClassType(scheduleId, req.getClassType())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SEAT_INVENTORY_NOT_FOUND));

        // 3. 재고 차감/수동 조정 시 Active 예약 개수 대조 무결성 검증 추가
        if (req.getRemainingSeats() != null) {
            long activeBookingsCount = flightBookingRepository.countActiveBookings(scheduleId, req.getClassType());
            
            // 조정하려는 남은 좌석(remainingSeats)이 전체 좌석에서 실시간 결제된 좌석 수량을 뺀 한도를 초과하여 음수 재고를 유발하는지 검사
            if (req.getRemainingSeats() > (inventory.getTotalSeats() - activeBookingsCount)) {
                log.warn("⚠️ Invalid seat control attempt. Tried to set remainingSeats={}, but max limit is {}",
                        req.getRemainingSeats(), inventory.getTotalSeats() - activeBookingsCount);
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }
            
            inventory.setRemainingSeats(req.getRemainingSeats());
        }

        // 4. 특별 가격 Override 조정
        if (req.getOverridePrice() != null) {
            inventory.setBasePrice(req.getOverridePrice());
        }

        SeatInventory savedInventory = seatInventoryRepository.save(inventory);
        log.info("🎉 Schedule control applied successfully. scheduleId={}, remainingSeats={}, price={}",
                scheduleId, savedInventory.getRemainingSeats(), savedInventory.getBasePrice());

        return SellerScheduleControlResponse.builder()
                .scheduleId(scheduleId)
                .classType(savedInventory.getClassType())
                .remainingSeats(savedInventory.getRemainingSeats())
                .currentPrice(savedInventory.getBasePrice())
                .build();
    }

    public List<FlightSchedule> getAllSchedules() {
        return flightScheduleRepository.findAll();
    }
}
