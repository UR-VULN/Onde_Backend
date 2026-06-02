package com.onde.api.application.lbs;

import com.onde.api.application.lbs.dto.PropertyMarkerDto;
import com.onde.api.application.lbs.dto.PropertyRegisterRequest;
import com.onde.api.application.lbs.dto.PropertyRegisterResponse;
import com.onde.api.application.lbs.dto.PropertySearchResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/properties", "/api/v1/property"}) // 프론트엔드 단수/복수형 오타 완전 수용
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyRegisterResponse>> registerProperty(
            @Valid @RequestBody PropertyRegisterRequest req,
            @LoginMember Long sellerId) {

        PropertyRegisterResponse response = propertyService.registerProperty(req, sellerId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "매물 등록 완료. 관리자 검증 후 지도에 노출됩니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PropertySearchResponse>> getProperties(
            @RequestParam("swLat") Double swLat,
            @RequestParam("swLng") Double swLng,
            @RequestParam("neLat") Double neLat,
            @RequestParam("neLng") Double neLng) {

        PropertySearchResponse response = propertyService.getPropertiesByBoundingBox(swLat, swLng, neLat, neLng);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
