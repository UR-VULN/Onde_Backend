package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationListDto;
import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final com.onde.core.repository.ReservationRepository reservationRepository;
    private final com.onde.core.repository.CarRepository carRepository;

    public AccommodationSearchResponse searchAccommodations(AccommodationSearchRequest request) {
        Long days = null;
        if (request.getCheckIn() != null && request.getCheckOut() != null) {
            days = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
            if (days <= 0) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if ("price_asc".equals(request.getSort())) {
            // Note: Sorting by min price across rooms/inventories is complex in JPA Sort.
            // For now, we will sort by id as a placeholder.
        } else if ("price_desc".equals(request.getSort())) {
            // Placeholder
        }

        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        Page<Accommodation> accommodationPage = accommodationRepository.searchAccommodations(
                ApprovalStatus.APPROVED,
                request.getRegion(),
                request.getCategory(),
                request.getCheckIn(),
                request.getCheckOut() != null ? request.getCheckOut().minusDays(1) : null,
                days,
                pageable);

        Long stayDays = days;
        List<AccommodationListDto> listDtos = accommodationPage.getContent().stream()
                .map(a -> AccommodationListDto.builder()
                        .accommodationId(a.getId())
                        .name(a.getName())
                        .category(a.getCategory())
                        .location(a.getLocation())
                        .thumbnailUrl(a.getThumbnailUrl())
                        .minPrice(resolveMinPrice(a, request))
                        .availableRooms(countAvailableRooms(a, request, stayDays))
                        .build())
                .collect(Collectors.toList());

        return AccommodationSearchResponse.builder()
                .accommodations(listDtos)
                .totalCount((int) accommodationPage.getTotalElements())
                .build();
    }

    private Integer countAvailableRooms(Accommodation accommodation, AccommodationSearchRequest request, Long days) {
        List<Room> rooms = roomRepository.findByAccommodationId(accommodation.getId());
        if (request.getCheckIn() == null || request.getCheckOut() == null || days == null) {
            return rooms.size();
        }
        LocalDate endDate = request.getCheckOut().minusDays(1);
        return (int) rooms.stream()
                .filter(room -> inventoryRepository.countAvailableDays(
                        ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate) >= days)
                .count();
    }

    private Integer resolveMinPrice(Accommodation accommodation, AccommodationSearchRequest request) {
        if (request.getCheckIn() == null || request.getCheckOut() == null) {
            return null;
        }

        LocalDate endDate = request.getCheckOut().minusDays(1);
        return roomRepository.findByAccommodationId(accommodation.getId()).stream()
                .flatMap(room -> inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                        ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate).stream())
                .filter(inventory -> inventory.getStock() != null && inventory.getStock() > 0)
                .map(Inventory::getBasePrice)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::intValue)
                .orElse(null);
    }

    public com.onde.core.entity.reservation.Reservation reserveRoom(Long userId, com.onde.api.application.accommodation.dto.RoomReservationRequest req) {
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ValidationException(ErrorCode.ROOM_NOT_FOUND));

        LocalDate endDate = req.getCheckOutDate().minusDays(1);
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.ROOM, room.getId(), req.getCheckInDate(), endDate);

        long days = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());
        if (inventories.size() < days) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE); // or INSUFFICIENT_STOCK
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Inventory inv : inventories) {
            if (inv.getStock() <= 0) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }
            totalPrice = totalPrice.add(inv.getBasePrice());
        }

        com.onde.core.entity.reservation.Reservation reservation = com.onde.core.entity.reservation.Reservation.builder()
                .userId(userId)
                .targetType(ReservationTarget.ROOM)
                .targetId(room.getId())
                .checkIn(req.getCheckInDate().atTime(15, 0))
                .checkOut(req.getCheckOutDate().atTime(11, 0))
                .totalPrice(totalPrice)
                .status(com.onde.core.entity.reservation.ReservationStatus.RESERVED)
                .build();
        
        return reservationRepository.save(reservation);
    }

    public com.onde.core.entity.reservation.Reservation reserveCar(Long userId, com.onde.api.application.accommodation.dto.CarReservationRequest req) {
        com.onde.core.entity.accommodation.Car car = carRepository.findById(req.getCarId())
                .orElseThrow(() -> new ValidationException(ErrorCode.CAR_NOT_FOUND));

        LocalDate endDate = req.getEndDate().minusDays(1);
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.CAR, car.getId(), req.getStartDate(), endDate);

        long days = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate());
        if (inventories.size() < days) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Inventory inv : inventories) {
            if (inv.getStock() <= 0) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }
            totalPrice = totalPrice.add(inv.getBasePrice());
        }

        com.onde.core.entity.reservation.Reservation reservation = com.onde.core.entity.reservation.Reservation.builder()
                .userId(userId)
                .targetType(ReservationTarget.CAR)
                .targetId(car.getId())
                .checkIn(req.getStartDate().atTime(15, 0))
                .checkOut(req.getEndDate().atTime(11, 0))
                .totalPrice(totalPrice)
                .status(com.onde.core.entity.reservation.ReservationStatus.RESERVED)
                .build();
        
        return reservationRepository.save(reservation);
    }
}
