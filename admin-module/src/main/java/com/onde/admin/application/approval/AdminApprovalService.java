package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.AdminApprovalRequest;
import com.onde.admin.application.approval.dto.AdminApprovalResponse;
import com.onde.admin.application.approval.dto.AdminPendingApprovalsResponse;
import com.onde.admin.application.approval.dto.ApprovalProcessRequest;
import com.onde.admin.application.approval.dto.ApprovalProcessResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.flight.FlightSchedule;
import com.onde.core.entity.insurance.InsuranceProduct;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.FlightScheduleRepository;
import com.onde.core.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminApprovalService {

    private final AccommodationRepository accommodationRepository;
    private final CarRepository carRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * [고도화] 본사 검수 대기 상태(PENDING_APPROVAL) 4대 상품 목록 통합 조회 대시보드
     */
    public AdminPendingApprovalsResponse getPendingApprovals(String category) {
        log.info("💼 Loading pending approvals for category={}", category);

        List<AdminPendingApprovalsResponse.PendingFlightDto> flights = new ArrayList<>();
        List<AdminPendingApprovalsResponse.PendingInsuranceDto> insurances = new ArrayList<>();
        // ※ 만약 숙소와 렌터카도 대시보드 DTO에 추가해야 한다면 여기에 List 선언 후 아래 분기문에서 적재 가능

        boolean isGlobal = category == null || category.trim().isBlank();
        boolean loadFlight = isGlobal || category.trim().equalsIgnoreCase("FLIGHT");
        boolean loadInsurance = isGlobal || category.trim().equalsIgnoreCase("INSURANCE");

        // 1. 항공 스케줄 대기 목록 로드
        if (loadFlight) {
            List<FlightSchedule> pendingSchedules = flightScheduleRepository.findByStatus(com.onde.core.entity.flight.ApprovalStatus.PENDING_APPROVAL);
            flights = pendingSchedules.stream()
                    .map(fs -> AdminPendingApprovalsResponse.PendingFlightDto.builder()
                            .scheduleId(fs.getId())
                            .flightNumber(fs.getFlightNumber())
                            .departureAirport(fs.getRoute().getDepartureAirport())
                            .arrivalAirport(fs.getRoute().getArrivalAirport())
                            .departureTime(fs.getDepartureTime())
                            .status(fs.getStatus())
                            .build())
                    .collect(Collectors.toList());
        }

        // 2. 여행자 보험 요율안 대기 목록 로드
        if (loadInsurance) {
            List<InsuranceProduct> pendingProducts = insuranceProductRepository.findByStatus(com.onde.core.entity.flight.ApprovalStatus.PENDING_APPROVAL);
            insurances = pendingProducts.stream()
                    .map(ip -> AdminPendingApprovalsResponse.PendingInsuranceDto.builder()
                            .productId(ip.getId())
                            .productName(ip.getProductName())
                            .baseDailyRate(ip.getBaseDailyRate())
                            .status(ip.getStatus())
                            .build())
                    .collect(Collectors.toList());
        }

        return AdminPendingApprovalsResponse.builder()
                .pendingFlights(flights)
                .pendingInsurances(insurances)
                .build();
    }

    /**
     * [컨트롤러 첫 번째 코드 대응] Request Body 객체(ApprovalProcessRequest) 기반 승인 처리
     */
    @Transactional
    public ApprovalProcessResponse processApproval(ApprovalProcessRequest request) {
        log.info("💼 Processing body-based approval for targetId={}", request.targetId());
        
        ApprovalStatus status = ApprovalStatus.valueOf(request.status().toUpperCase());

        Accommodation accommodation = accommodationRepository.findById(request.targetId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INTERNAL_SERVER_ERROR)); // 기존 RuntimeException 고도화

        // 팀원의 엔티티 메서드 또는 필드 형태에 맞춰 상태 반영 (팀원 코드가 스네이크/카멜 케이스 에넘을 동기화하게 바인딩)
        accommodation.setApprovalStatus(status);
        accommodationRepository.save(accommodation);

        return new ApprovalProcessResponse(
                accommodation.getId(), 
                status,
                "심사가 성공적으로 반영되었습니다."
        );
    }

    /**
     * [컨트롤러 두 번째 코드 대응] PathVariable(requestId) 및 카테고리 기반 상품 검수 최종 처리
     */
    @Transactional
    public AdminApprovalResponse processApproval(Long requestId, AdminApprovalRequest req) {
        log.info("💼 Processing path-based approval requestId={}, category={}, decision={}",
                requestId, req.getCategory(), req.getDecision());

        String category = req.getCategory().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        if (category.equals("FLIGHT")) {
            FlightSchedule schedule = flightScheduleRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

            schedule.setStatus(req.getDecision());
            if (req.getDecision() == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                schedule.setRejectReason(req.getRejectReason());
            }
            flightScheduleRepository.save(schedule);

            // 항공 상품 승인(APPROVED) 성공 시 관련된 Redis 항공 검색 캐시 즉시 일괄 무효화
            if (req.getDecision() == com.onde.core.entity.flight.ApprovalStatus.APPROVED) {
                clearRedisCache("flightSearch*");
            }

        } else if (category.equals("INSURANCE")) {
            InsuranceProduct product = insuranceProductRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

            product.setStatus(req.getDecision());
            if (req.getDecision() == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                product.setRejectReason(req.getRejectReason());
            }
            insuranceProductRepository.save(product);

        } else {
            throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        log.info("🎉 Approval decision applied successfully. requestId={}, decision={}", requestId, req.getDecision());

        return AdminApprovalResponse.builder()
                .requestId(requestId)
                .decision(req.getDecision())
                .updatedAt(now)
                .build();
    }

    /**
     * 숙소 및 차량용 대기 목록 조회 편의 메서드 (구용성 컴포넌트 유지)
     */
    public List<Accommodation> getPendingAccommodations() {
        return accommodationRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    public List<Car> getPendingCars() {
        return carRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    /**
     * Redis 캐시 무효화 내부 공통 메서드
     */
    private void clearRedisCache(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🧹 [CACHE EVICT] Successfully invalidated {} keys for pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("🧹 [CACHE EVICT ERROR] Failed to clear Redis caches: {}", e.getMessage());
        }
    }
}