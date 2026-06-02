package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationResponse;
import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import com.onde.core.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.onde.core.entity.reservation.Reservation;

import com.onde.core.support.ApiResponse;

@RestController
@RequestMapping({"/api/v1/cars", "/api/v1/rental_cars"})
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final ReservationService reservationService;
    private final CarRepository carRepository;

    // 렌터카 목록 및 검색
    @GetMapping({"", "/search"})
    public ResponseEntity<ApiResponse<CarSearchResponse>> search(CarSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(carService.searchCars(request), "렌터카 조회가 완료되었습니다."));
    }

    // 렌터카 예약 생성
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<CarReservationResponse>> reserveCar(@RequestBody CarReservationRequest request) {
        Reservation reservation = reservationService.reserveCar(request);
        String modelName = carRepository.findById(request.getCarId())
                .map(com.onde.core.entity.accommodation.Car::getModelName)
                .orElse(null);
        CarReservationResponse response = new CarReservationResponse(
                reservation.getId(),
                reservation.getTargetType(),
                reservation.getTargetId(),
                modelName,
                reservation.getCheckIn().toLocalDate(),
                reservation.getCheckOut().toLocalDate(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "렌터카 예약이 완료되었습니다."));
    }
}
