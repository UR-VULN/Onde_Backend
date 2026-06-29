package com.onde.api.application.lbs;

import com.onde.api.application.lbs.dto.PropertyMarkerDto;
import com.onde.api.application.lbs.dto.PropertyRegisterRequest;
import com.onde.api.application.lbs.dto.PropertyRegisterResponse;
import com.onde.api.application.lbs.dto.PropertySearchResponse;
import com.onde.core.entity.lbs.Property;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PropertyRepository;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.RoomRepository;
import com.onde.core.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final MemberRepository memberRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public PropertyRegisterResponse registerProperty(PropertyRegisterRequest req, Long sellerId) {
        // 1. 좌표 정밀도 검증 (소수점 4자리 이상)
        validateCoordinate(req.getLatitude(), req.getLongitude());

        // 2. 판매자 존재 유무 확인 (논리 FK 검증)
        if (!memberRepository.existsById(sellerId)) {
            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 3. 매물 저장 (isVerified = false로 시작)
        Property property = Property.builder()
                .sellerId(sellerId)
                .addressName(req.getAddressName())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .isVerified(false)
                .build();

        Property savedProperty = propertyRepository.save(property);
        return PropertyRegisterResponse.from(savedProperty);
    }

    public PropertySearchResponse getPropertiesByBoundingBox(Double swLat, Double swLng, Double neLat, Double neLng) {
        // 1. 남서(sw) ~ 북동(ne) 범위 내 isVerified = true 인 매물 리스트 조회
        List<Property> properties = propertyRepository.findVerifiedByBoundingBox(swLat, swLng, neLat, neLng);

        if (properties.isEmpty()) {
            return PropertySearchResponse.builder()
                    .markers(List.of())
                    .totalCount(0)
                    .build();
        }

        // 2. 매물의 addressName 목록 추출
        List<String> addressNames = properties.stream()
                .map(Property::getAddressName)
                .distinct()
                .toList();

        // 3. 주소(이름) 기준 숙소 목록을 한 번에 Bulk 조회 (N+1 방지)
        List<com.onde.core.entity.accommodation.Accommodation> accommodations = 
                accommodationRepository.findByNameIn(addressNames);

        // 4. 숙소 이름 기준 Map으로 캐싱 (메모리 O(1) 조회를 위함)
        java.util.Map<String, com.onde.core.entity.accommodation.Accommodation> accMap = accommodations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.onde.core.entity.accommodation.Accommodation::getName,
                        a -> a,
                        (existing, replacement) -> existing
                ));

        // 5. 숙소 ID 목록 추출 → 최저가 Bulk 조회 (N+1 방지, 쿼리 1회)
        List<Long> accommodationIds = accommodations.stream()
                .map(com.onde.core.entity.accommodation.Accommodation::getId)
                .toList();

        List<Object[]> minPriceRows = inventoryRepository.findMinPriceByAccommodationIds(accommodationIds);

        // 6. accommodationId → 최저가 Map 구성 (메모리 O(1) 조회)
        java.util.Map<Long, Integer> priceMap = minPriceRows.stream()
                .filter(row -> row[0] != null && row[1] != null)
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((java.math.BigDecimal) row[1]).intValue(),
                        (e, r) -> e
                ));

        // 7. 메모리 맵을 통해 O(1) 초고속 매핑
        List<PropertyMarkerDto> markers = properties.stream()
                .map(p -> {
                    PropertyMarkerDto dto = PropertyMarkerDto.from(p);
                    com.onde.core.entity.accommodation.Accommodation acc = accMap.get(p.getAddressName());
                    if (acc != null) {
                        dto.setAccommodationId(acc.getId());
                        dto.setThumbnailUrl(acc.getThumbnailUrl());
                        dto.setMinPrice(priceMap.get(acc.getId()));
                    }
                    return dto;
                })
                .toList();

        return PropertySearchResponse.builder()
                .markers(markers)
                .totalCount(markers.size())
                .build();
    }

    private void validateCoordinate(Double lat, Double lng) {
        String latStr = String.valueOf(lat);
        String lngStr = String.valueOf(lng);

        int latDecimal = latStr.contains(".") ? latStr.split("\\.")[1].length() : 0;
        int lngDecimal = lngStr.contains(".") ? lngStr.split("\\.")[1].length() : 0;

        if (latDecimal < 4 || lngDecimal < 4) {
            throw new ValidationException(ErrorCode.INVALID_COORDINATE);
        }
    }
}
