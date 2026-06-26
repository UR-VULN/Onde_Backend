package com.onde.api.application.accommodation;

import com.onde.api.application.auth.support.MemberIdBindingValidator;
import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.notification.NotificationService;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.event.AdminBookingCancelEvent;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.CarRepository;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CarRepository carRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public boolean checkAvailability(ReservationTarget targetType, Long targetId, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                targetType, targetId, startDate, endDate.minusDays(1));
        
        if (inventories.size() != days) return false;
        
        return inventories.stream().allMatch(i -> i.getStock() > 0);
    }

    @Transactional
    public Reservation reserveRoom(Long memberId, RoomReservationRequest request) {
        MemberIdBindingValidator.rejectForgedMemberId(memberId, request.getMemberId());

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROOM_NOT_FOUND));

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.ROOM, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate().minusDays(1));

        if (inventories.size() != nights) {
            throw new ValidationException(ErrorCode.INVENTORY_NOT_AVAILABLE);
        }

        BigDecimal basePriceSum = BigDecimal.ZERO;
        for (Inventory inventory : inventories) {
            if (inventory.getStock() <= 0) {
                throw new ValidationException(ErrorCode.INVENTORY_NOT_AVAILABLE);
            }
            inventory.setStock(inventory.getStock() - 1);
            basePriceSum = basePriceSum.add(inventory.getBasePrice());
        }

        // 인원 초과 확인 (최대 수용 인원 기준)
        if (request.getGuests() != null && request.getGuests() > room.getCapacity()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        BigDecimal totalPrice = basePriceSum;
        
        // 원단위 절사 (10원 단위로 반올림/절사)
        totalPrice = totalPrice.divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(10));

        Reservation reservation = new Reservation();
        reservation.setUserId(memberId);
        reservation.setTargetType(ReservationTarget.ROOM);
        reservation.setTargetId(request.getRoomId());
        reservation.setCheckIn(request.getCheckInDate().atTime(15, 0));
        reservation.setCheckOut(request.getCheckOutDate().atTime(11, 0));
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setTotalPrice(totalPrice);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation reserveCar(Long memberId, CarReservationRequest request) {
        MemberIdBindingValidator.rejectForgedMemberId(memberId, request.getMemberId());

        carRepository.findById(request.getCarId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CAR_NOT_FOUND));

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.CAR, request.getCarId(), request.getStartDate(), request.getEndDate().minusDays(1));

        if (inventories.size() != days) {
            throw new ValidationException(ErrorCode.INVENTORY_NOT_AVAILABLE);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Inventory inventory : inventories) {
            if (inventory.getStock() <= 0) {
                throw new ValidationException(ErrorCode.INVENTORY_NOT_AVAILABLE);
            }
            inventory.setStock(inventory.getStock() - 1);
            totalPrice = totalPrice.add(inventory.getBasePrice());
        }

        // 원단위 절사
        totalPrice = totalPrice.divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(10));

        Reservation reservation = new Reservation();
        reservation.setUserId(memberId);
        reservation.setTargetType(ReservationTarget.CAR);
        reservation.setTargetId(request.getCarId());
        reservation.setCheckIn(request.getStartDate().atStartOfDay());
        reservation.setCheckOut(request.getEndDate().atStartOfDay());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setTotalPrice(totalPrice);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUserId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.RESERVATION_NOT_OWNER);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Restore inventory stock
        LocalDate startDate = reservation.getCheckIn().toLocalDate();
        LocalDate endDate = reservation.getCheckOut().toLocalDate().minusDays(1);

        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                reservation.getTargetType(), reservation.getTargetId(), startDate, endDate);

        for (Inventory inventory : inventories) {
            inventory.setStock(inventory.getStock() + 1);
        }

        Reservation savedReservation = reservationRepository.save(reservation);
        eventPublisher.publishEvent(new AdminBookingCancelEvent(
                this,
                savedReservation.getId(),
                savedReservation.getUserId(),
                savedReservation.getTargetType().name()));
        notificationService.sendSinglePush(
                savedReservation.getUserId(),
                "예약이 취소되었습니다.",
                "예약 번호 " + savedReservation.getId() + "의 취소가 완료되었습니다.");
        return savedReservation;
    }
}
