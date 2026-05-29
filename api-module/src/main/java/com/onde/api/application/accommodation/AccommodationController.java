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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    /**
     * 숙소 목록 및 검색
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AccommodationSearchResponse>> search(
            AccommodationSearchRequest request) {
        
        AccommodationSearchResponse response = accommodationService.searchAccommodations(request);
        return ResponseEntity.ok(ApiResponse.success(response, "숙소 조회가 완료되었습니다."));
    }
}