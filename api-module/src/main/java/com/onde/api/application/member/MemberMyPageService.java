package com.onde.api.application.member;

import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.insurance.InsurancePolicy;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberMyPageService {

    private final FlightBookingRepository flightBookingRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;

    public MyPageListResponse<MyPageFlightBookingResponse> getMyFlightBookings(Long userId, String status, Pageable pageable) {
        Page<FlightBooking> pageResult;

        if (status == null || status.isBlank()) {
            pageResult = flightBookingRepository.findByUserId(userId, pageable);
        } else {
            BookingStatus bookingStatus;
            try {
                bookingStatus = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid booking status filter: {}", status);
                return MyPageListResponse.<MyPageFlightBookingResponse>builder()
                        .bookings(List.of())
                        .totalCount(0)
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .build();
            }
            pageResult = flightBookingRepository.findByUserIdAndStatus(userId, bookingStatus, pageable);
        }

        List<MyPageFlightBookingResponse> dtoList = pageResult.getContent().stream()
                .map(fb -> MyPageFlightBookingResponse.builder()
                        .bookingId(fb.getId())
                        .bookingCode(fb.getBookingCode())
                        .flightNumber(fb.getFlightSchedule() != null ? fb.getFlightSchedule().getFlightNumber() : null)
                        .origin(fb.getFlightSchedule() != null && fb.getFlightSchedule().getRoute() != null ? fb.getFlightSchedule().getRoute().getDepartureAirport() : null)
                        .destination(fb.getFlightSchedule() != null && fb.getFlightSchedule().getRoute() != null ? fb.getFlightSchedule().getRoute().getArrivalAirport() : null)
                        .departureTime(fb.getFlightSchedule() != null ? fb.getFlightSchedule().getDepartureTime() : null)
                        .arrivalTime(fb.getFlightSchedule() != null ? fb.getFlightSchedule().getArrivalTime() : null)
                        .seatClass(fb.getSeatClass())
                        .passengerName(fb.getPassenger() != null ? fb.getPassenger().getPassengerName() : null)
                        .totalPrice(fb.getTotalPrice())
                        .status(fb.getStatus())
                        .build())
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageFlightBookingResponse>builder()
                .bookings(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MyPageListResponse<MyPageInsurancePolicyResponse> getMyInsurancePolicies(Long userId, String status, Pageable pageable) {
        Page<InsurancePolicy> pageResult;

        if (status == null || status.isBlank()) {
            pageResult = insurancePolicyRepository.findByUserId(userId, pageable);
        } else {
            pageResult = insurancePolicyRepository.findByUserIdAndStatus(userId, status.trim().toUpperCase(), pageable);
        }

        List<MyPageInsurancePolicyResponse> dtoList = pageResult.getContent().stream()
                .map(ip -> MyPageInsurancePolicyResponse.builder()
                        .policyId(ip.getId())
                        .policyCode(ip.getPolicyCode())
                        .productName(ip.getInsuranceProduct() != null ? ip.getInsuranceProduct().getProductName() : null)
                        .insuredName(ip.getInsuredName())
                        .startDate(ip.getStartDate() != null ? ip.getStartDate().toString() : null)
                        .endDate(ip.getEndDate() != null ? ip.getEndDate().toString() : null)
                        .coverageLevel(ip.getCoverageLevel())
                        .totalPremium(ip.getTotalPremium())
                        .status(ip.getStatus())
                        .build())
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageInsurancePolicyResponse>builder()
                .bookings(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }
}
