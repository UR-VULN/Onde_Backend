package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.core.entity.accommodation.RoomInventory;
import com.onde.core.entity.accommodation.CarInventory;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.RoomInventoryRepository;
import com.onde.core.repository.CarInventoryRepository;
import com.onde.core.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RoomInventoryRepository roomInventoryRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation reserveRoom(RoomReservationRequest request) {
        List<RoomInventory> inventories = roomInventoryRepository.findByRoom_RoomIdAndInventoryDateBetween(
                request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate().minusDays(1));

        for (RoomInventory inventory : inventories) {
            if (inventory.getAvailableQuantity() <= 0) {
                throw new RuntimeException("No rooms available for date: " + inventory.getInventoryDate());
            }
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - 1);
        }

        Reservation reservation = new Reservation();
        reservation.setMemberId(request.getMemberId());
        reservation.setTargetType(ReservationTarget.STAY);
        reservation.setTargetId(request.getRoomId());
        reservation.setStartDate(request.getCheckInDate());
        reservation.setEndDate(request.getCheckOutDate());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setTotalPrice(request.getTotalPrice());
        reservation.setCreatedAt(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation reserveCar(CarReservationRequest request) {
        List<CarInventory> inventories = carInventoryRepository.findByCar_CarIdAndInventoryDateBetween(
                request.getCarId(), request.getStartDate(), request.getEndDate().minusDays(1));

        for (CarInventory inventory : inventories) {
            if (inventory.getAvailableQuantity() <= 0) {
                throw new RuntimeException("No cars available for date: " + inventory.getInventoryDate());
            }
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - 1);
        }

        Reservation reservation = new Reservation();
        reservation.setMemberId(request.getMemberId());
        reservation.setTargetType(ReservationTarget.CAR);
        reservation.setTargetId(request.getCarId());
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setTotalPrice(request.getTotalPrice());
        reservation.setCreatedAt(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }
}
