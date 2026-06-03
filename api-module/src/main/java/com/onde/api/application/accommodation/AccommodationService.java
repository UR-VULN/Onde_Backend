package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.*;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

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
        } else if ("price_desc".equals(request.getSort())) {
            // Placeholder
        }

        List<Accommodation> accommodations = accommodationRepository.searchAccommodations(
                ApprovalStatus.APPROVED,
                request.getRegion(),
                request.getCategory(),
                request.getGuests(),
                request.getCheckIn(),
                request.getCheckOut() != null ? request.getCheckOut().minusDays(1) : null,
                days,
                sort);

        Long stayDays = days;
        List<AccommodationListDto> listDtos = accommodations.stream()
                .map(a -> AccommodationListDto.builder()
                        .accommodationId(a.getId())
                        .name(a.getName())
                        .category(a.getCategory())
                        .location(a.getLocation())
                        .thumbnailUrl(a.getThumbnailUrl())
                        .minPrice(resolveMinPrice(a, request))
                        .availableRooms(countAvailableRooms(a, request, stayDays))
                        .build())
                .filter(dto -> dto.getAvailableRooms() > 0)
                .collect(Collectors.toList());

        return AccommodationSearchResponse.builder()
                .accommodations(listDtos)
                .totalCount(listDtos.size())
                .build();
    }

    @Transactional
    public ReservationResponse reserveRoom(Long memberId, RoomReservationRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROOM_NOT_FOUND));

        // 1. 인원 수 검증
        if (request.getGuests() != null && request.getGuests() > room.getMaxCapacity()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate startDate = request.getCheckInDate();
        LocalDate endDate = request.getCheckOutDate().minusDays(1);
        long days = ChronoUnit.DAYS.between(startDate, request.getCheckOutDate());

        // 2. 재고 확인 및 차감
        List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.ROOM, room.getId(), startDate, endDate);

        if (inventories.size() < days) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_AVAILABLE);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Inventory inventory : inventories) {
            if (inventory.getStock() <= 0) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_AVAILABLE);
            }
            inventory.setStock(inventory.getStock() - 1);
            
            // 3. 요금 계산 (기본가 + 인원 추가 금리)
            BigDecimal dayPrice = inventory.getBasePrice();
            if (request.getGuests() != null && request.getGuests() > room.getStandardCapacity()) {
                int extraGuests = request.getGuests() - room.getStandardCapacity();
                BigDecimal extraCharge = room.getSurcharge().multiply(BigDecimal.valueOf(extraGuests));
                dayPrice = dayPrice.add(extraCharge);
            }
            totalAmount = totalAmount.add(dayPrice);
        }

        // 4. 예약 생성
        Reservation reservation = Reservation.builder()
                .userId(memberId)
                .targetType(ReservationTarget.ROOM)
                .targetId(room.getId())
                .checkIn(startDate.atStartOfDay())
                .checkOut(request.getCheckOutDate().atStartOfDay())
                .totalPrice(totalAmount)
                .status(ReservationStatus.RESERVED)
                .build();

        reservationRepository.save(reservation);

        return new ReservationResponse(
                reservation.getId(),
                reservation.getTargetType(),
                reservation.getTargetId(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );
    }

    private Integer countAvailableRooms(Accommodation accommodation, AccommodationSearchRequest request, Long days) {
        List<Room> rooms = roomRepository.findByAccommodationId(accommodation.getId());
        if (request.getCheckIn() == null || request.getCheckOut() == null || days == null) {
            return rooms.size();
        }

        Integer guestCount = request.getGuests() != null ? request.getGuests() : 1;
        LocalDate endDate = request.getCheckOut().minusDays(1);
        
        return (int) rooms.stream()
                .filter(room -> room.getMaxCapacity() >= guestCount)
                .filter(room -> inventoryRepository.countAvailableDays(
                        ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate) >= days)
                .count();
    }

    private Integer resolveMinPrice(Accommodation accommodation, AccommodationSearchRequest request) {
        if (request.getCheckIn() == null || request.getCheckOut() == null) {
            return null;
        }

        Integer guestCount = request.getGuests() != null ? request.getGuests() : 1;
        LocalDate endDate = request.getCheckOut().minusDays(1);

        return roomRepository.findByAccommodationId(accommodation.getId()).stream()
                .filter(room -> room.getMaxCapacity() >= guestCount)
                .map(room -> {
                    List<Inventory> inventories = inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                            ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate);
                    
                    if (inventories.isEmpty()) return null;

                    BigDecimal totalPrice = BigDecimal.ZERO;
                    for (Inventory inventory : inventories) {
                        if (inventory.getStock() == null || inventory.getStock() <= 0) return null;
                        
                        BigDecimal dayPrice = inventory.getBasePrice();
                        if (guestCount > room.getStandardCapacity()) {
                            int extraGuests = guestCount - room.getStandardCapacity();
                            dayPrice = dayPrice.add(room.getSurcharge().multiply(BigDecimal.valueOf(extraGuests)));
                        }
                        totalPrice = totalPrice.add(dayPrice);
                    }
                    return totalPrice;
                })
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::intValue)
                .orElse(null);
    }
}
