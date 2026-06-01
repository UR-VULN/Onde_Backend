package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarListDto;
import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;

    public CarSearchResponse searchCars(CarSearchRequest request) {
        Long days = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (request.getPickup() != null && request.getReturnTime() != null) {
            startDate = request.getPickup().toLocalDate();
            endDate = request.getReturnTime().toLocalDate();
            days = ChronoUnit.DAYS.between(startDate, endDate);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if ("price_asc".equals(request.getSort())) {
            // Placeholder
        } else if ("price_desc".equals(request.getSort())) {
            // Placeholder
        }

        List<Car> cars = carRepository.searchCars(
                ApprovalStatus.APPROVED, 
                request.getCarType(),
                request.getFuelType(),
                request.getCapacity(),
                startDate,
                endDate != null ? endDate.minusDays(1) : null,
                days,
                sort);

        List<CarListDto> listDtos = cars.stream()
                .map(c -> CarListDto.builder()
                        .id(c.getId())
                        .modelName(c.getModelName())
                        .carType(c.getCarType())
                        .fuelType(c.getFuelType())
                        .capacity(c.getCapacity())
                        .price(50000) // Placeholder
                        .build())
                .collect(Collectors.toList());

        return CarSearchResponse.builder()
                .cars(listDtos)
                .build();
    }

    public List<Car> getCarsBySellerId(Long sellerId) {
        return carRepository.findBySellerId(sellerId);
    }
}
