package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationResponse;
import com.onde.api.application.accommodation.dto.ReservationCancelResponse;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.repository.CarRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final CarRepository carRepository;

    //숙소 예약 생성
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveRoom(
            @LoginMember Long memberId,
            @RequestBody RoomReservationRequest req) {
        if (req.getMemberId() == null) {
            req.setMemberId(memberId);
        }
        Reservation reservation = reservationService.reserveRoom(req);
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
                .body(ApiResponse.success(response, "예약이 완료되었습니다."));
    }

    //렌터카 예약 생성
    @PostMapping("/cars")
    public ResponseEntity<ApiResponse<CarReservationResponse>> reserveCar(
            @LoginMember Long memberId,
            @RequestBody CarReservationRequest req) {
        if (req.getMemberId() == null) {
            req.setMemberId(memberId);
        }
        Reservation reservation = reservationService.reserveCar(req);
        String modelName = carRepository.findById(req.getCarId())
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
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "렌터카 예약이 성공적으로 완료되었습니다."));
    }

    // 예약 취소
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(
            @LoginMember Long memberId,
            @PathVariable("id") Long id) {
        
        Reservation reservation = reservationService.cancelReservation(id, memberId);
        ReservationCancelResponse response = new ReservationCancelResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.success(response, "예약이 취소되었습니다."));
    }
}
