package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {
    private final AccommodationService accommodationService;

    @GetMapping("/search")
    public ResponseEntity<AccommodationSearchResponse> search(AccommodationSearchRequest request) {
        return ResponseEntity.ok(accommodationService.searchAccommodations(request));
    }
}
