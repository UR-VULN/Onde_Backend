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
        // 남서(sw) ~ 북동(ne) 범위 내 isVerified = true 인 매물 리스트 조회
        List<Property> properties = propertyRepository.findVerifiedByBoundingBox(swLat, swLng, neLat, neLng);

        List<PropertyMarkerDto> markers = properties.stream()
                .map(PropertyMarkerDto::from)
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
