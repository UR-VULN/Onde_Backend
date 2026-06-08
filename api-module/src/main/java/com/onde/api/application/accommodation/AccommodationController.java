package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.onde.api.security.LoginMember;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accommodations") 
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final ReservationService reservationService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AccommodationSearchResponse>> search(
            AccommodationSearchRequest request) {

        AccommodationSearchResponse response = accommodationService.searchAccommodations(request);
        return ResponseEntity.ok(ApiResponse.success(response, "숙소 조회가 완료되었습니다."));
    }

    @PostMapping("/reservations/rooms")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveRoom(
            @LoginMember Long userId,
            @RequestBody RoomReservationRequest req) {

        if (req.getMemberId() == null) {
            req.setMemberId(userId);
        }
        com.onde.core.entity.reservation.Reservation reservation = reservationService.reserveRoom(req);
        ReservationResponse response = new ReservationResponse(
                reservation.getId(),
                reservation.getTargetType(),
                reservation.getTargetId(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "객실 예약이 성공적으로 완료되었습니다."));
    }

    @PostMapping("/reservations/cars")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveCar(
            @LoginMember Long userId,
            @RequestBody CarReservationRequest req) {

        if (req.getMemberId() == null) {
            req.setMemberId(userId);
        }
        com.onde.core.entity.reservation.Reservation reservation = reservationService.reserveCar(req);
        ReservationResponse response = new ReservationResponse(
                reservation.getId(),
                reservation.getTargetType(),
                reservation.getTargetId(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "렌터카 예약이 성공적으로 완료되었습니다."));
    }
}
