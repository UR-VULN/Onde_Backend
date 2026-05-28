package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.ApprovalProcessRequest;
import com.onde.admin.application.approval.dto.ApprovalProcessResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {
    
    private final AccommodationRepository accommodationRepository;
    private final CarRepository carRepository;

    // =================================================================
    // 1. 단일 통합 승인/반려 프로세스 (컨트롤러에서 호출하는 메서드)
    // =================================================================
    @Transactional
    public ApprovalProcessResponse processApproval(ApprovalProcessRequest request) {
        // record 문법에 맞게 request.status() 와 request.targetId() 를 사용합니다.
        ApprovalStatus status = ApprovalStatus.valueOf(request.status());

        Accommodation accommodation = accommodationRepository.findById(request.targetId())
                .orElseThrow(() -> new RuntimeException("해당 숙소를 찾을 수 없습니다."));

        accommodation.setApprovalStatus(status);

        // 엔터티의 식별자는 getId()로 가져옵니다.
        return new ApprovalProcessResponse(
                accommodation.getId(), 
                status,
                "심사가 성공적으로 반영되었습니다."
        );
    }

    // =================================================================
    // 2. 대기 목록 조회 (성능 개선: findAll() 대신 커스텀 Repository 메서드 사용 권장)
    // =================================================================
    public List<Accommodation> getPendingAccommodations() {
        return accommodationRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    public List<Car> getPendingCars() {
        return carRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }
}