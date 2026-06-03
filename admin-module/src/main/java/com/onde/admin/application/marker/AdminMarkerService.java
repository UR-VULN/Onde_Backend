package com.onde.admin.application.marker;

import com.onde.admin.application.marker.dto.AdminMarkerRequest;
import com.onde.admin.application.marker.dto.AdminMarkerResponse;
import com.onde.core.entity.lbs.GuideMarker;
import com.onde.core.repository.GuideMarkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMarkerService {

    private final GuideMarkerRepository guideMarkerRepository;

    @Transactional
    public AdminMarkerResponse registerMarker(AdminMarkerRequest req, String adminId) {
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
