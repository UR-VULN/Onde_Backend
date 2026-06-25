package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryBulkUpdateRequest;
import com.onde.api.application.accommodation.dto.RoomInventoryBulkUpdateResponse;
import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterResponse;
import com.onde.api.config.MockS3Uploader;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Accommodation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import com.onde.core.repository.RoomRepository;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.ErrorCode;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerAccommodationController {
    private final SellerAccommodationService sellerAccommodationService;
    private final com.onde.core.repository.AccommodationRepository accommodationRepository;
    private final com.onde.core.repository.RoomRepository roomRepository;
    private final MockS3Uploader s3Uploader;

    // 판매자 등록 숙소 신규 등록 API (주소 규격 보정 포함)

    @PostMapping(value = "/accommodations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SellerAccommodationRegisterResponse>> registerJson(
            @RequestBody SellerAccommodationRegisterRequest request,
            @LoginMember Long sellerId) {
        request.setSellerId(sellerId);
        Long id = sellerAccommodationService.registerAccommodation(request);
        SellerAccommodationRegisterResponse response = buildRegisterResponse(id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "숙소 등록 신청 완료. 관리자 승인 후 노출됩니다."));
    }

    @PostMapping(value = "/accommodations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SellerAccommodationRegisterResponse>> registerMultipart(
            @RequestParam(required = false) MultipartFile thumbnail,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam String location,
            @RequestParam(required = false) String businessLicense,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String rooms,
            @LoginMember Long sellerId) {
        SellerAccommodationRegisterRequest request = new SellerAccommodationRegisterRequest();
        request.setSellerId(sellerId);
        request.setName(name);
        request.setDescription(description);
        request.setCategory(category);
        request.setLocation(location);
        request.setBusinessLicense(businessLicense);
        request.setLatitude(latitude);
        request.setLongitude(longitude);

        if (rooms != null && !rooms.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<SellerAccommodationRegisterRequest.RoomRegisterRequest> roomsList = objectMapper.readValue(
                        rooms,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SellerAccommodationRegisterRequest.RoomRegisterRequest.class)
                );
                request.setRooms(roomsList);
            } catch (Exception e) {
                // ignore
            }
        }

        request.setThumbnailUrl(s3Uploader.upload(thumbnail, "accommodations"));

        Long id = sellerAccommodationService.registerAccommodation(request);
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
        verifyRoomOwnership(sellerId, roomId);
        sellerAccommodationService.updateRoomInventories(roomId, requests);
        return ResponseEntity.ok(ApiResponse.success(null, "객실 재고 정보가 성공적으로 수정되었습니다."));
    }

    @PutMapping("/inventories/rooms")
    public ResponseEntity<ApiResponse<RoomInventoryBulkUpdateResponse>> updateInventories(
            @LoginMember Long sellerId,
            @Valid @RequestBody RoomInventoryBulkUpdateRequest request) {
        verifyRoomOwnership(sellerId, request.roomId());
        RoomInventoryBulkUpdateResponse response = sellerAccommodationService.updateRoomInventoriesBulk(request);
        return ResponseEntity.ok(ApiResponse.success(response, "객실 재고/가격 수정 성공"));
    }

    private void verifyRoomOwnership(Long sellerId, Long roomId) {
        roomRepository.findById(roomId)
                .ifPresentOrElse(
                        room -> {
                            if (!room.getAccommodation().getSellerId().equals(sellerId)) {
                                throw new ForbiddenException(ErrorCode.FORBIDDEN);
                            }
                        },
                        () -> {
                            throw new IllegalArgumentException("존재하지 않는 객실입니다.");
                        }
                );
    }
}
