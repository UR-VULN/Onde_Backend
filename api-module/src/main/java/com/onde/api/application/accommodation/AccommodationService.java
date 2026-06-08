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
        } else if ("price_desc".equals(request.getSort())) {
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
                .filter(room -> request.getGuests() == null || room.getCapacity() >= request.getGuests())
                .filter(room -> inventoryRepository.countAvailableDays(
                        ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate) >= days)
                .count();
    }

    private Integer resolveMinPrice(Accommodation accommodation, AccommodationSearchRequest request) {
        if (request.getCheckIn() == null || request.getCheckOut() == null) {
            return null;
        }

        LocalDate endDate = request.getCheckOut().minusDays(1);
        int guests = request.getGuests() != null ? request.getGuests() : 2;

        return roomRepository.findByAccommodationId(accommodation.getId()).stream()
                .filter(room -> request.getGuests() == null || room.getCapacity() >= guests)
                .flatMap(room -> {
                    BigDecimal dailySurcharge = guests > room.getBaseCapacity()
                            ? room.getSurchargePerPerson().multiply(BigDecimal.valueOf(guests - room.getBaseCapacity()))
                            : BigDecimal.ZERO;

                    return inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                            ReservationTarget.ROOM, room.getId(), request.getCheckIn(), endDate).stream()
                            .filter(inventory -> inventory.getStock() != null && inventory.getStock() > 0)
                            .map(inventory -> inventory.getBasePrice().add(dailySurcharge));
                })
                .min(BigDecimal::compareTo)
                .map(BigDecimal::intValue)
                .orElse(null);
    }
    }
