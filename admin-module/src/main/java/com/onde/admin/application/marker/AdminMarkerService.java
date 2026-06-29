package com.onde.admin.application.marker;

import com.onde.admin.application.marker.dto.AdminMarkerRequest;
import com.onde.admin.application.marker.dto.AdminMarkerResponse;
import com.onde.core.entity.lbs.GuideMarker;
import com.onde.core.repository.GuideMarkerRepository;
import com.onde.core.config.UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMarkerService {

    private final GuideMarkerRepository guideMarkerRepository;
    private final UrlValidator urlValidator;

    @Transactional
    public AdminMarkerResponse registerMarker(AdminMarkerRequest req, String adminId) {
        // [보안 강화 - ADMIN-2 / SSRF 방어] 마커 등록 시 외부 URL에 대한 SSRF 검증 수행
        if (req.getUrl() != null && !req.getUrl().isBlank()) {
            urlValidator.validateUrl(req.getUrl());
        }

        GuideMarker marker = GuideMarker.builder()
                .name(req.getName())
                .category(req.getCategory())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .createdBy(adminId)
                .build();

        GuideMarker savedMarker = guideMarkerRepository.save(marker);
        return AdminMarkerResponse.from(savedMarker);
    }
}
