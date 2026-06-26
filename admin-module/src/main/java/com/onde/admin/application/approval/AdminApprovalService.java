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
import com.onde.core.repository.PropertyRepository;
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
    private final PropertyRepository propertyRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * [고도화] 본사 검수 대기 상태(PENDING_APPROVAL) 4대 상품 목록 통합 조회 대시보드
     */
    public AdminPendingApprovalsResponse getPendingApprovals(String category) {
        log.info("💼 Loading pending approvals for category={}", category);

        List<AdminPendingApprovalsResponse.PendingFlightDto> flights = new ArrayList<>();
        List<AdminPendingApprovalsResponse.PendingInsuranceDto> insurances = new ArrayList<>();
        // ※ 만약 숙소와 렌터카도 대시보드 DTO에 추가해야 한다면 여기에 List 선언 후 아래 분기문에서 적재 가능

        String normalizedCategory = category == null ? "" : category.trim();
        boolean isGlobal = normalizedCategory.isBlank();
        boolean loadFlight = isGlobal || "FLIGHT".equalsIgnoreCase(normalizedCategory);
        boolean loadInsurance = isGlobal || "INSURANCE".equalsIgnoreCase(normalizedCategory);

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
        String approvalType = normalizeApprovalType(request.approvalType());
        String status = normalizeApprovalAction(request.action(), request.status());
        LocalDateTime processedAt = LocalDateTime.now();

        log.info("💼 Processing body-based approval. approvalType={}, targetId={}, status={}",
                approvalType, request.targetId(), status);

        switch (approvalType) {
            case "FLIGHT" -> {
                FlightSchedule schedule = flightScheduleRepository.findById(request.targetId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));
                com.onde.core.entity.flight.ApprovalStatus flightStatus =
                        com.onde.core.entity.flight.ApprovalStatus.valueOf(status);
                schedule.setStatus(flightStatus);
                if (flightStatus == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                    schedule.setRejectReason(request.rejectReason());
                }
                flightScheduleRepository.save(schedule);
                if (flightStatus == com.onde.core.entity.flight.ApprovalStatus.APPROVED) {
                    clearRedisCache("flightSearch*");
                }
            }
            case "INSURANCE" -> {
                InsuranceProduct product = insuranceProductRepository.findById(request.targetId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
                com.onde.core.entity.flight.ApprovalStatus insuranceStatus =
                        com.onde.core.entity.flight.ApprovalStatus.valueOf(status);
                product.setStatus(insuranceStatus);
                if (insuranceStatus == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                    product.setRejectReason(request.rejectReason());
                }
                insuranceProductRepository.save(product);
            }
            case "ACCOMMODATION" -> {
                Accommodation accommodation = accommodationRepository.findById(request.targetId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.INTERNAL_SERVER_ERROR));
                accommodation.setApprovalStatus(ApprovalStatus.valueOf(status));
                accommodationRepository.save(accommodation);
                
                if (ApprovalStatus.valueOf(status) == ApprovalStatus.APPROVED) {
                    List<com.onde.core.entity.lbs.Property> properties = propertyRepository.findByAddressName(accommodation.getName());
                    for (com.onde.core.entity.lbs.Property p : properties) {
                        p.setIsVerified(true);
                        propertyRepository.save(p);
                    }
                }
            }
            case "CAR" -> {
                Car car = carRepository.findById(request.targetId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.INTERNAL_SERVER_ERROR));
                car.setApprovalStatus(ApprovalStatus.valueOf(status));
                carRepository.save(car);
            }
            default -> throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new ApprovalProcessResponse(approvalType, request.targetId(), status, processedAt);
    }

    /**
     * [컨트롤러 두 번째 코드 대응] PathVariable(requestId) 및 카테고리 기반 상품 검수 최종 처리
     */
    @Transactional
    public AdminApprovalResponse processApproval(Long requestId, AdminApprovalRequest req) {
        com.onde.core.entity.flight.ApprovalStatus decision = req.getResolvedDecision();
        if (decision == null) {
            throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        log.info("💼 Processing path-based approval requestId={}, category={}, decision={}",
                requestId, req.getCategory(), decision);

        String category = resolvePathApprovalCategory(requestId, req.getCategory());
        LocalDateTime now = LocalDateTime.now();

        if (category.equals("FLIGHT")) {
            FlightSchedule schedule = flightScheduleRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

            schedule.setStatus(decision);
            if (decision == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                schedule.setRejectReason(req.getResolvedRejectReason());
            }
            flightScheduleRepository.save(schedule);

            // 항공 상품 승인(APPROVED) 성공 시 관련된 Redis 항공 검색 캐시 즉시 일괄 무효화
            if (decision == com.onde.core.entity.flight.ApprovalStatus.APPROVED) {
                clearRedisCache("flightSearch*");
            }

        } else if (category.equals("INSURANCE")) {
            InsuranceProduct product = insuranceProductRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

            product.setStatus(decision);
            if (decision == com.onde.core.entity.flight.ApprovalStatus.REJECTED) {
                product.setRejectReason(req.getResolvedRejectReason());
            }
            insuranceProductRepository.save(product);

        } else {
            throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        log.info("🎉 Approval decision applied successfully. requestId={}, decision={}", requestId, decision);

        return AdminApprovalResponse.builder()
                .requestId(requestId)
                .status(decision)
                .processedAt(now)
                .build();
    }

    private String normalizeApprovalType(String approvalType) {
        if (approvalType == null || approvalType.isBlank()) {
            return "ACCOMMODATION";
        }
        return approvalType.trim().toUpperCase();
    }

    private String normalizeApprovalAction(String action, String status) {
        String value = action != null && !action.isBlank() ? action : status;
        if (value == null || value.isBlank()) {
            throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("APPROVE")) {
            return "APPROVED";
        }
        if (normalized.equals("REJECT")) {
            return "REJECTED";
        }
        return normalized;
    }

    private String resolvePathApprovalCategory(Long requestId, String category) {
        if (category != null && !category.isBlank()) {
            return category.trim().toUpperCase();
        }
        if (flightScheduleRepository.existsById(requestId)) {
            return "FLIGHT";
        }
        if (insuranceProductRepository.existsById(requestId)) {
            return "INSURANCE";
        }
        throw new com.onde.core.exception.ValidationException(ErrorCode.INVALID_INPUT_VALUE);
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
