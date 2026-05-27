package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarListDto;
import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;

    public CarSearchResponse searchCars(CarSearchRequest request) {
        List<Car> cars = carRepository.searchCars(
                ApprovalStatus.APPROVED, request.getLocation(), request.getCarType());

        List<CarListDto> listDtos = cars.stream()
                .map(c -> CarListDto.builder()
                        .carId(c.getCarId())
                        .modelName(c.getModelName())
                        .carType(c.getCarType())
                        .location(c.getLocation())
                        .price(50000) // Placeholder
                        .build())
                .toList();

        return CarSearchResponse.builder()
                .cars(listDtos)
                .build();
    }
}
