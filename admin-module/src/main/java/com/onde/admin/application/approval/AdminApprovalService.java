package com.onde.admin.application.approval;

import com.onde.admin.application.approval.dto.AdminApprovalRequest;
import com.onde.admin.application.approval.dto.AdminApprovalResponse;
import com.onde.admin.application.approval.dto.AdminPendingApprovalsResponse;
import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.entity.flight.FlightSchedule;
import com.onde.core.entity.insurance.InsuranceProduct;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.FlightScheduleRepository;
import com.onde.core.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminApprovalService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * [Day 7] 본사 검수 대기 상태(PENDING_APPROVAL) 상품 목록 통합 조회 dashboard
     */
    public AdminPendingApprovalsResponse getPendingApprovals(String category) {
        log.info("💼 Loading pending approvals for category={}", category);

        List<AdminPendingApprovalsResponse.PendingFlightDto> flights = new ArrayList<>();
        List<AdminPendingApprovalsResponse.PendingInsuranceDto> insurances = new ArrayList<>();

        boolean loadFlight = category == null || category.trim().equalsIgnoreCase("FLIGHT");
        boolean loadInsurance = category == null || category.trim().equalsIgnoreCase("INSURANCE");

        // 1. 항공 스케줄 대기 목록 로드
        if (loadFlight) {
            List<FlightSchedule> pendingSchedules = flightScheduleRepository.findByStatus(ApprovalStatus.PENDING_APPROVAL);
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
            List<InsuranceProduct> pendingProducts = insuranceProductRepository.findByStatus(ApprovalStatus.PENDING_APPROVAL);
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
     * [Day 7] 상품 검수 최종 처리 (승인 및 반려)
     * 비즈니스 룰: 승인(APPROVED) 성공 시 트랜잭션 내에서 관련된 Redis 캐시 즉시 와일드카드 무효화
     */
    @Transactional
    public AdminApprovalResponse processApproval(Long requestId, AdminApprovalRequest req) {
        log.info("💼 Processing approval request requestId={}, category={}, decision={}",
                requestId, req.getCategory(), req.getDecision());

        String category = req.getCategory().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        if (category.equals("FLIGHT")) {
            // 1. 항공 스케줄 데이터 확보
            FlightSchedule schedule = flightScheduleRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_SCHEDULE_NOT_FOUND));

            schedule.setStatus(req.getDecision());
            if (req.getDecision() == ApprovalStatus.REJECTED) {
                schedule.setRejectReason(req.getRejectReason());
            }

            flightScheduleRepository.save(schedule);

            // 2. 상품 승인(APPROVED) 성공 시 관련된 Redis 항공 검색 캐시 즉시 일괄 와일드카드 무효화
            if (req.getDecision() == ApprovalStatus.APPROVED) {
                try {
                    Set<String> keys = redisTemplate.keys("flightSearch*");
                    if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                        log.info("🧹 [CACHE EVICT] Successfully invalidated {} cached flight searches", keys.size());
                    }
                } catch (Exception e) {
                    log.error("🧹 [CACHE EVICT ERROR] Failed to clear Redis flight caches: {}", e.getMessage());
                }
            }

        } else if (category.equals("INSURANCE")) {
            // 3. 보험 상품 요율안 확보
            InsuranceProduct product = insuranceProductRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

            product.setStatus(req.getDecision());
            if (req.getDecision() == ApprovalStatus.REJECTED) {
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
}
