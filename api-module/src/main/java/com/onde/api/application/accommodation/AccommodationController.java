package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import com.onde.api.application.accommodation.dto.RoomReservationRequest;
import com.onde.api.application.accommodation.dto.CarReservationRequest;
import com.onde.api.application.accommodation.dto.ReservationResponse;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.onde.api.security.LoginMember;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/accommodations") // 👈 프로젝트 공통 표준 v1 및 도메인 네임 스페이스로 통합
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final ReservationService reservationService;

    /**
     * [첫 번째 코드 스펙] 숙소 검색 및 조회 (비로그인 개방 경로)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AccommodationSearchResponse>> search(
            @Valid @ModelAttribute AccommodationSearchRequest request) {

        AccommodationSearchResponse response = accommodationService.searchAccommodations(request);
        return ResponseEntity.ok(ApiResponse.success(response, "숙소 조회가 완료되었습니다."));
    }

    /**
     * [두 번째 코드 스펙] 객실 예약 (시큐리티 세션 연동)
     */
    @PostMapping("/reservations/rooms")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveRoom(
            @LoginMember Long userId,
            @Valid @RequestBody RoomReservationRequest req) {

        com.onde.core.entity.reservation.Reservation reservation = reservationService.reserveRoom(userId, req);
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

    /**
     * [두 번째 코드 스펙] 렌터카 예약 (시큐리티 세션 연동)
     */
    @PostMapping("/reservations/cars")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveCar(
            @LoginMember Long userId,
            @Valid @RequestBody CarReservationRequest req) {

        com.onde.core.entity.reservation.Reservation reservation = reservationService.reserveCar(userId, req);
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
