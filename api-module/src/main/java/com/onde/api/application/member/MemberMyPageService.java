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
    private final com.onde.core.repository.ReservationRepository reservationRepository;
    private final com.onde.core.repository.RoomRepository roomRepository;
    private final com.onde.core.repository.CarRepository carRepository;

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

    public MyPageListResponse<MyPageRoomReservationResponse> getMyRoomReservations(Long userId, String status, Pageable pageable) {
        Page<com.onde.core.entity.reservation.Reservation> pageResult;

        com.onde.core.entity.reservation.ReservationTarget targetType = com.onde.core.entity.reservation.ReservationTarget.ROOM;
        if (status == null || status.isBlank()) {
            pageResult = reservationRepository.findByUserIdAndTargetType(userId, targetType, pageable);
        } else {
            com.onde.core.entity.reservation.ReservationStatus resStatus;
            try {
                resStatus = com.onde.core.entity.reservation.ReservationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MyPageListResponse.<MyPageRoomReservationResponse>builder()
                        .reservations(List.of())
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
                            .checkIn(res.getCheckIn())
                            .checkOut(res.getCheckOut())
                            .totalPrice(res.getTotalPrice())
                            .status(res.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageRoomReservationResponse>builder()
                .reservations(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public MyPageListResponse<MyPageCarReservationResponse> getMyCarReservations(Long userId, String status, Pageable pageable) {
        Page<com.onde.core.entity.reservation.Reservation> pageResult;

        com.onde.core.entity.reservation.ReservationTarget targetType = com.onde.core.entity.reservation.ReservationTarget.CAR;
        if (status == null || status.isBlank()) {
            pageResult = reservationRepository.findByUserIdAndTargetType(userId, targetType, pageable);
        } else {
            com.onde.core.entity.reservation.ReservationStatus resStatus;
            try {
                resStatus = com.onde.core.entity.reservation.ReservationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MyPageListResponse.<MyPageCarReservationResponse>builder()
                        .reservations(List.of())
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
                            .totalPrice(res.getTotalPrice())
                            .status(res.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return MyPageListResponse.<MyPageCarReservationResponse>builder()
                .reservations(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }
}
