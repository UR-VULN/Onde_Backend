package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.onde.core.entity.reservation.Reservation;

@RestController
@RequestMapping("/api/rental_cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final ReservationService reservationService;

    // 검색 API
    @GetMapping("/search")
    public ResponseEntity<CarSearchResponse> search(CarSearchRequest request) {
        return ResponseEntity.ok(carService.searchCars(request));
    }

    // 예약 API
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@RequestBody CarReservationRequest request) {
        // 1. Service 로직을 실행하고 Reservation 엔터티를 반환받습니다.
        Reservation reservation = reservationService.reserveCar(request);
        
        // 2. 엔터티의 데이터를 꺼내서 클라이언트 응답용 DTO로 포장합니다.
        ReservationResponse response = new ReservationResponse(
            reservation.getId(),
            reservation.getStatus(),
            "예약 신청이 완료되었습니다."
        );
        
        // 3. 포장된 DTO를 반환합니다.
        return ResponseEntity.ok(response);
    }
}