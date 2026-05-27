package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.CarSearchRequest;
import com.onde.api.application.accommodation.dto.CarSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @GetMapping("/search")
    public ResponseEntity<CarSearchResponse> search(CarSearchRequest request) {
        return ResponseEntity.ok(carService.searchCars(request));
    }
}
