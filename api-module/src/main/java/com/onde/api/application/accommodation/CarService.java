package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarListDto;
import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;
    private final InventoryRepository inventoryRepository;

    public CarSearchResponse searchCars(CarSearchRequest request) {
        Long days = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (request.getPickup() != null && request.getReturnTime() != null) {
            startDate = request.getPickup().toLocalDate();
            endDate = request.getReturnTime().toLocalDate();
            days = ChronoUnit.DAYS.between(startDate, endDate);
            if (days <= 0) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if ("price_asc".equals(request.getSort())) {
            
        } else if ("price_desc".equals(request.getSort())) {
            
        }

        String location = request.getLocation();
        if (location != null && (location.isBlank() || "전체".equals(location))) {
            location = null;
        }

        String carType = request.getCarType();
        if (carType != null && (carType.isBlank() || "전체 차량".equals(carType) || "ALL".equals(carType))) {
            carType = null;
        }

        List<Car> cars = carRepository.searchCars(
                ApprovalStatus.APPROVED, 
                location,
                carType,
                startDate,
                endDate != null ? endDate.minusDays(1) : null,
                days,
                sort);

        LocalDate searchStartDate = startDate;
        LocalDate searchEndDate = endDate;
        List<CarListDto> listDtos = cars.stream()
                .map(c -> CarListDto.builder()
                        .carId(c.getId())
                        .modelName(c.getModelName())
                        .carType(c.getCarType())
                        .licensePlate(c.getLicensePlate())
                        .dailyPrice(resolveDailyPrice(c, searchStartDate, searchEndDate))
                        .location(c.getLocation())
                        .available(true)
                        .build())
                .collect(Collectors.toList());

        return CarSearchResponse.builder()
                .cars(listDtos)
                .totalCount(listDtos.size())
                .build();
    }

    private Integer resolveDailyPrice(Car car, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                        ReservationTarget.CAR, car.getId(), startDate, endDate.minusDays(1)).stream()
                .filter(inventory -> inventory.getStock() != null && inventory.getStock() > 0)
                .map(Inventory::getBasePrice)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::intValue)
                .orElse(null);
    }

    public List<Car> getCarsBySellerId(Long sellerId) {
        return carRepository.findBySellerId(sellerId);
    }
}
