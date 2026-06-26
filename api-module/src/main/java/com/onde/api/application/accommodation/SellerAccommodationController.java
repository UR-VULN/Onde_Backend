package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryBulkUpdateRequest;
import com.onde.api.application.accommodation.dto.RoomInventoryBulkUpdateResponse;
import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationMultipartForm;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.validation.MultipartInputValidator;
import org.springframework.validation.annotation.Validated;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterResponse;
import com.onde.api.config.MockS3Uploader;
import com.onde.core.entity.accommodation.Accommodation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.onde.api.application.accommodation.support.SellerPropertyOwnershipService;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerAccommodationController {
    private final SellerAccommodationService sellerAccommodationService;
    private final SellerPropertyOwnershipService sellerPropertyOwnershipService;
    private final com.onde.core.repository.AccommodationRepository accommodationRepository;
    private final MockS3Uploader s3Uploader;

    // 판매자 등록 숙소 신규 등록 API (주소 규격 보정 포함)

    @PostMapping(value = "/accommodations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SellerAccommodationRegisterResponse>> registerJson(
            @Valid @RequestBody SellerAccommodationRegisterRequest request,
            @LoginMember Long sellerId) {
        Long id = sellerAccommodationService.registerAccommodation(sellerId, request);
        SellerAccommodationRegisterResponse response = buildRegisterResponse(id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "숙소 등록 신청 완료. 관리자 승인 후 노출됩니다."));
    }

    @PostMapping(value = "/accommodations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SellerAccommodationRegisterResponse>> registerMultipart(
            @RequestParam(required = false) MultipartFile thumbnail,
            @Valid @ModelAttribute SellerAccommodationMultipartForm form,
            @LoginMember Long sellerId) {
        MultipartInputValidator.validateOptionalImage(thumbnail);

        SellerAccommodationRegisterRequest request = new SellerAccommodationRegisterRequest();
        request.setName(form.getName());
        request.setDescription(form.getDescription());
        request.setCategory(form.getCategory());
        request.setLocation(form.getLocation());
        request.setBusinessLicense(form.getBusinessLicense());
        request.setLatitude(form.getLatitude());
        request.setLongitude(form.getLongitude());

        if (form.getRooms() != null && !form.getRooms().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
                List<SellerAccommodationRegisterRequest.RoomRegisterRequest> roomsList = objectMapper.readValue(
                        form.getRooms(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SellerAccommodationRegisterRequest.RoomRegisterRequest.class)
                );
                request.setRooms(roomsList);
            } catch (Exception e) {
                throw new IllegalArgumentException("객실 정보 JSON 형식이 올바르지 않습니다.");
            }
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            request.setThumbnailUrl(s3Uploader.upload(thumbnail, "accommodations"));
        }

        Long id = sellerAccommodationService.registerAccommodation(sellerId, request);
        SellerAccommodationRegisterResponse response = buildRegisterResponse(id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "숙소 등록 신청 완료. 관리자 승인 후 노출됩니다."));
    }

    private SellerAccommodationRegisterResponse buildRegisterResponse(Long id) {
        Accommodation accommodation = accommodationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("숙소 등록 정보를 찾을 수 없습니다."));
        return SellerAccommodationRegisterResponse.builder()
                .accommodationId(accommodation.getId())
                .name(accommodation.getName())
                .thumbnailUrl(accommodation.getThumbnailUrl())
                .approvalStatus(accommodation.getApprovalStatus())
                .build();
    }

    // 판매자 등록 숙소 목록 조회 API
    @GetMapping("/accommodations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccommodations(
            @LoginMember Long sellerId) {
        List<com.onde.core.entity.accommodation.Accommodation> list = sellerAccommodationService.getAccommodations(sellerId);
        
        List<Map<String, Object>> mapped = list.stream().map(a -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", a.getId());
            item.put("name", a.getName());
            String status = "ACTIVE";
            if (a.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.PENDING) {
                status = "PENDING";
            } else if (a.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", 120000);
            return item;
        }).toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("accommodations", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 숙소 목록 조회가 성공적으로 완료되었습니다."));
    }

      // 객실 재고/가격 수정 (특정 객실 대상)
    @PutMapping("/accommodations/rooms/{roomId}/inventory")
    public ResponseEntity<ApiResponse<Void>> updateRoomInventory(
            @LoginMember Long sellerId,
            @PathVariable Long roomId,
            @RequestBody List<@Valid RoomInventoryUpdateRequest> requests) {
        sellerPropertyOwnershipService.assertSellerOwnsRoom(sellerId, roomId);
        sellerAccommodationService.updateRoomInventories(roomId, requests);
        return ResponseEntity.ok(ApiResponse.success(null, "객실 재고 정보가 성공적으로 수정되었습니다."));
    }

    @PutMapping("/inventories/rooms")
    public ResponseEntity<ApiResponse<RoomInventoryBulkUpdateResponse>> updateInventories(
            @LoginMember Long sellerId,
            @Valid @RequestBody RoomInventoryBulkUpdateRequest request) {
        sellerPropertyOwnershipService.assertSellerOwnsRoom(sellerId, request.roomId());
        RoomInventoryBulkUpdateResponse response = sellerAccommodationService.updateRoomInventoriesBulk(request);
        return ResponseEntity.ok(ApiResponse.success(response, "객실 재고/가격 수정 성공"));
    }
}
