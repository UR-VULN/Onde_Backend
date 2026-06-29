package com.onde.api.application.member;

import com.onde.api.application.member.dto.MemberProfileResponse;
import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import com.onde.api.application.member.dto.ProfileUpdateRequestDto;
import com.onde.api.application.member.dto.SellerProfileResponse;
import com.onde.api.application.member.dto.SellerProfileUpdateRequest;
import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.insurance.InsurancePolicy;
import com.onde.core.entity.insurance.InsurancePolicyStatus;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberMyPageService {

    private final FlightBookingRepository flightBookingRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final com.onde.core.repository.ReservationRepository reservationRepository;
    private final com.onde.core.repository.RoomRepository roomRepository;
    private final com.onde.core.repository.CarRepository carRepository;
    private final com.onde.core.repository.MemberRepository memberRepository;
    private final com.onde.core.repository.PaymentRepository paymentRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public MyPageListResponse<MyPageFlightBookingResponse> getMyFlightBookings(Long userId, String status, Pageable pageable) {
        Page<FlightBooking> pageResult;

        if (status == null || status.isBlank()) {
            pageResult = flightBookingRepository.findByUserIdAndStatus(userId, BookingStatus.CONFIRMED, pageable);
        } else {
            BookingStatus bookingStatus;
            try {
                bookingStatus = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid booking status filter: {}", status);
                return MyPageListResponse.<MyPageFlightBookingResponse>builder()
                        .content(List.of())
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
                        .departureTime(fb.getFlightSchedule() != null && fb.getFlightSchedule().getDepartureTime() != null ? fb.getFlightSchedule().getDepartureTime().toLocalDate().toString() : null)
                        .arrivalTime(fb.getFlightSchedule() != null && fb.getFlightSchedule().getArrivalTime() != null ? fb.getFlightSchedule().getArrivalTime().toLocalDate().toString() : null)
                        .seatClass(fb.getSeatClass())
                        .passengerName(fb.getPassenger() != null ? fb.getPassenger().getPassengerName() : null)
                        .totalPrice(paymentRepository.findFirstByReservationIdAndReservationTypeOrderByIdDesc(fb.getId(), "FLIGHT")
                                .map(com.onde.core.entity.payment.Payment::getPgAmount)
                                .orElse(fb.getTotalPrice()))
                        .status(fb.getStatus())
                        .build())
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageFlightBookingResponse>builder()
                .content(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MyPageListResponse<MyPageInsurancePolicyResponse> getMyInsurancePolicies(Long userId, String status, Pageable pageable) {
        Page<InsurancePolicy> pageResult;

        if (status == null || status.isBlank()) {
            pageResult = insurancePolicyRepository.findByUserIdAndStatusIn(userId, List.of(InsurancePolicyStatus.ACTIVE, InsurancePolicyStatus.EXPIRED), pageable);
        } else {
            InsurancePolicyStatus policyStatus;
            try {
                policyStatus = InsurancePolicyStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid insurance status filter: {}", status);
                return MyPageListResponse.<MyPageInsurancePolicyResponse>builder()
                        .content(List.of())
                        .totalCount(0)
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .build();
            }
            pageResult = insurancePolicyRepository.findByUserIdAndStatus(userId, policyStatus, pageable);
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
                        .totalPremium(paymentRepository.findFirstByReservationIdAndReservationTypeOrderByIdDesc(ip.getId(), "INSURANCE")
                                .map(com.onde.core.entity.payment.Payment::getPgAmount)
                                .orElse(ip.getTotalPremium()))
                        .status(ip.getStatus() != null ? ip.getStatus().name() : null)
                        .build())
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageInsurancePolicyResponse>builder()
                .content(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MyPageListResponse<MyPageRoomReservationResponse> getMyRoomReservations(Long userId, String status, Pageable pageable) {
        Page<com.onde.core.entity.reservation.Reservation> pageResult;

        com.onde.core.entity.reservation.ReservationTarget targetType = com.onde.core.entity.reservation.ReservationTarget.ROOM;
        if (status == null || status.isBlank()) {
            pageResult = reservationRepository.findByUserIdAndTargetTypeAndStatusIn(userId, targetType, List.of(com.onde.core.entity.reservation.ReservationStatus.CONFIRMED, com.onde.core.entity.reservation.ReservationStatus.COMPLETED), pageable);
        } else {
            com.onde.core.entity.reservation.ReservationStatus resStatus;
            try {
                resStatus = com.onde.core.entity.reservation.ReservationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MyPageListResponse.<MyPageRoomReservationResponse>builder()
                        .content(List.of())
                        .totalCount(0)
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .build();
            }
            pageResult = reservationRepository.findByUserIdAndTargetTypeAndStatus(userId, targetType, resStatus, pageable);
        }

        List<MyPageRoomReservationResponse> dtoList = pageResult.getContent().stream()
                .map(res -> {
                    String accommodationName = null;
                    String roomName = null;
                    com.onde.core.entity.accommodation.Room room = roomRepository.findById(res.getTargetId()).orElse(null);
                    if (room != null) {
                        roomName = room.getName();
                        if (room.getAccommodation() != null) {
                            accommodationName = room.getAccommodation().getName();
                        }
                    }
                    return MyPageRoomReservationResponse.builder()
                            .reservationId(res.getId())
                            .accommodationName(accommodationName)
                            .roomName(roomName)
                            .checkIn(res.getCheckIn() != null ? res.getCheckIn().toLocalDate().toString() : null)
                            .checkOut(res.getCheckOut() != null ? res.getCheckOut().toLocalDate().toString() : null)
                            .totalPrice(paymentRepository.findFirstByReservationIdAndReservationTypeOrderByIdDesc(res.getId(), "ROOM")
                                    .map(com.onde.core.entity.payment.Payment::getPgAmount)
                                    .orElse(res.getTotalPrice()))
                            .status(res.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageRoomReservationResponse>builder()
                .content(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MyPageListResponse<MyPageCarReservationResponse> getMyCarReservations(Long userId, String status, Pageable pageable) {
        Page<com.onde.core.entity.reservation.Reservation> pageResult;

        com.onde.core.entity.reservation.ReservationTarget targetType = com.onde.core.entity.reservation.ReservationTarget.CAR;
        if (status == null || status.isBlank()) {
            pageResult = reservationRepository.findByUserIdAndTargetTypeAndStatusIn(userId, targetType, List.of(com.onde.core.entity.reservation.ReservationStatus.CONFIRMED, com.onde.core.entity.reservation.ReservationStatus.COMPLETED), pageable);
        } else {
            com.onde.core.entity.reservation.ReservationStatus resStatus;
            try {
                resStatus = com.onde.core.entity.reservation.ReservationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MyPageListResponse.<MyPageCarReservationResponse>builder()
                        .content(List.of())
                        .totalCount(0)
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .build();
            }
            pageResult = reservationRepository.findByUserIdAndTargetTypeAndStatus(userId, targetType, resStatus, pageable);
        }

        List<MyPageCarReservationResponse> dtoList = pageResult.getContent().stream()
                .map(res -> {
                    String modelName = null;
                    String carType = null;
                    com.onde.core.entity.accommodation.Car car = carRepository.findById(res.getTargetId()).orElse(null);
                    if (car != null) {
                        modelName = car.getModelName();
                        carType = car.getCarType();
                    }
                    return MyPageCarReservationResponse.builder()
                            .reservationId(res.getId())
                            .modelName(modelName)
                            .carType(carType)
                            .checkIn(res.getCheckIn() != null ? res.getCheckIn().toLocalDate().toString() : null)
                            .checkOut(res.getCheckOut() != null ? res.getCheckOut().toLocalDate().toString() : null)
                            .totalPrice(paymentRepository.findFirstByReservationIdAndReservationTypeOrderByIdDesc(res.getId(), "CAR")
                                    .map(com.onde.core.entity.payment.Payment::getPgAmount)
                                    .orElse(res.getTotalPrice()))
                            .status(res.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageCarReservationResponse>builder()
                .content(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MemberInfoResponse getMyInfo(Long userId) {
        com.onde.core.entity.member.Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.MEMBER_NOT_FOUND));

        return MemberInfoResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .role(member.getRole() != null ? member.getRole().name() : null)
                .provider(member.getProvider() != null ? member.getProvider().name() : null)
                .status(member.getStatus() != null ? member.getStatus().name() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }

    @Transactional
    public void cancelFlightBooking(Long userId, Long bookingId) {
        FlightBooking booking = flightBookingRepository.findById(bookingId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getUserId().equals(userId)) {
            throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.FORBIDDEN);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.INVALID_INPUT_VALUE);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        eventPublisher.publishEvent(new com.onde.core.event.AdminBookingCancelEvent(this, bookingId, userId, "FLIGHT"));
    }

    @Transactional
    public void cancelInsurancePolicy(Long userId, Long policyId) {
        InsurancePolicy policy = insurancePolicyRepository.findById(policyId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.INSURANCE_POLICY_NOT_FOUND));
        if (!policy.getUserId().equals(userId)) {
            throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.FORBIDDEN);
        }
        policy.setStatus(com.onde.core.entity.insurance.InsurancePolicyStatus.CANCELLED);
        eventPublisher.publishEvent(new com.onde.core.event.AdminBookingCancelEvent(this, policyId, userId, "INSURANCE"));
    }

    public MemberProfileResponse getProfile(Long userId) {
        com.onde.core.entity.member.Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.MEMBER_NOT_FOUND));

        return MemberProfileResponse.builder()
                .email(member.getEmail())
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .nickname(member.getNickname())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDto requestDto) {
        com.onde.core.entity.member.Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 체크 (자신의 닉네임이 아닌 경우에만)
        if (requestDto.getNickname() != null && !requestDto.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(requestDto.getNickname())) {
                throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.NICKNAME_DUPLICATION);
            }
        }

        // 기본 정보 업데이트
        member.updateProfile(requestDto.getName(), requestDto.getPhoneNumber(), requestDto.getNickname());

        // 비밀번호 변경 요청이 있는 경우에만 처리
        if (requestDto.getNewPassword() != null && !requestDto.getNewPassword().isBlank()) {
            member.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
        }
    }

    public SellerProfileResponse getSellerProfile(Long userId) {
        com.onde.core.entity.member.Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.MEMBER_NOT_FOUND));

        return SellerProfileResponse.builder()
                .email(member.getEmail())
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .nickname(member.getNickname())
                .build();
    }

    @Transactional
    public void updateSellerProfile(Long userId, SellerProfileUpdateRequest request) {
        com.onde.core.entity.member.Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 체크
        if (request.getNickname() != null && !request.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(request.getNickname())) {
                throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.NICKNAME_DUPLICATION);
            }
        }

        // 프로필 정보 업데이트
        member.updateProfile(request.getName(), request.getPhoneNumber(), request.getNickname());

        // 비밀번호 변경 처리
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            member.updatePassword(passwordEncoder.encode(request.getPassword()));
        }
    }
}
