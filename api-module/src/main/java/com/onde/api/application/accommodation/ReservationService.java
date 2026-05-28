package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public boolean checkAvailability(ReservationTarget targetType, Long targetId, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                targetType, targetId, startDate, endDate.minusDays(1));
        
        if (inventories.size() != days) return false;
        
        return inventories.stream().allMatch(i -> i.getStock() > 0);
    }

    @Transactional
    public Reservation reserveRoom(RoomReservationRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.ROOM, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate().minusDays(1));

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (inventories.size() != nights) {
            throw new RuntimeException("No inventory found for all selected dates");
        }

        BigDecimal basePriceSum = BigDecimal.ZERO;
        for (Inventory inventory : inventories) {
            if (inventory.getStock() <= 0) {
                throw new RuntimeException("No rooms available for date: " + inventory.getDate());
            }
            inventory.setStock(inventory.getStock() - 1);
            basePriceSum = basePriceSum.add(inventory.getBasePrice());
        }

        // 인원 추가 비용 계산: (투숙 인원 - 기준 인원) * 추가 요금 * 박수
        BigDecimal extraFee = BigDecimal.ZERO;
        if (request.getGuests() != null && room.getBaseCapacity() != null && request.getGuests() > room.getBaseCapacity()) {
            BigDecimal extraPerNight = room.getExtraPersonFee() != null ? room.getExtraPersonFee() : BigDecimal.ZERO;
            extraFee = extraPerNight.multiply(BigDecimal.valueOf(request.getGuests() - room.getBaseCapacity()))
                                     .multiply(BigDecimal.valueOf(nights));
        }

        BigDecimal totalPrice = basePriceSum.add(extraFee);
        
        // 원단위 절사 (10원 단위로 반올림/절사)
        totalPrice = totalPrice.divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(10));

        Reservation reservation = new Reservation();
        reservation.setUserId(request.getMemberId());
        reservation.setTargetType(ReservationTarget.ROOM);
        reservation.setTargetId(request.getRoomId());
        reservation.setCheckIn(request.getCheckInDate().atStartOfDay());
        reservation.setCheckOut(request.getCheckOutDate().atStartOfDay());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setTotalPrice(totalPrice);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation reserveCar(CarReservationRequest request) {
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.CAR, request.getCarId(), request.getStartDate(), request.getEndDate().minusDays(1));

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (inventories.size() != days) {
            throw new RuntimeException("No inventory found for all selected dates");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Inventory inventory : inventories) {
            if (inventory.getStock() <= 0) {
                throw new RuntimeException("No cars available for date: " + inventory.getDate());
            }
            inventory.setStock(inventory.getStock() - 1);
            totalPrice = totalPrice.add(inventory.getBasePrice());
        }

        // 원단위 절사
        totalPrice = totalPrice.divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(10));

        Reservation reservation = new Reservation();
        reservation.setUserId(request.getMemberId());
        reservation.setTargetType(ReservationTarget.CAR);
        reservation.setTargetId(request.getCarId());
        reservation.setCheckIn(request.getStartDate().atStartOfDay());
        reservation.setCheckOut(request.getEndDate().atStartOfDay());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setTotalPrice(totalPrice);

        return reservationRepository.save(reservation);
    }
}
