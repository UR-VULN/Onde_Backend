package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    @PostMapping("/reservations/rooms")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveRoom(
            @RequestBody RoomReservationRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        
        // TODO: 객실 예약 비즈니스 로직 연동
        ReservationResponse response = new ReservationResponse();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "객실 예약이 성공적으로 완료되었습니다."));
    }

    @PostMapping("/reservations/cars")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveCar(
            @RequestBody CarReservationRequest req,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader) {
        
        // TODO: 렌터카 예약 비즈니스 로직 연동
        ReservationResponse response = new ReservationResponse();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "렌터카 예약이 성공적으로 완료되었습니다."));
    }
}
