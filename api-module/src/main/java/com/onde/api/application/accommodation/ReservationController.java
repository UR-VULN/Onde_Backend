package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 숙소 예약 생성
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RoomReservationRequest req) {
        
        Reservation reservation = reservationService.reserveRoom(req);
        ReservationResponse response = new ReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                "객실 예약이 성공적으로 완료되었습니다."
        );
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "객실 예약이 성공적으로 완료되었습니다."));
    }

    /**
     * 렌터카 예약 생성
     */
    @PostMapping("/cars")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveCar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CarReservationRequest req) {
        
        Reservation reservation = reservationService.reserveCar(req);
        ReservationResponse response = new ReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                "렌터카 예약이 성공적으로 완료되었습니다."
        );
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "렌터카 예약이 성공적으로 완료되었습니다."));
    }

    /**
     * 예약 취소
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        // TODO: memberId 추출 로직 필요 (현재는 서비스에서 처리하도록 id만 넘기거나 userDetails에서 추출)
        reservationService.cancelReservation(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "예약이 성공적으로 취소되었습니다."));
    }
}
