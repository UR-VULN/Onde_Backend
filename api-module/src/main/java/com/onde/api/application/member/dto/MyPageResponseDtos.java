package com.onde.api.application.member.dto;

import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.SeatClass;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MyPageResponseDtos {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageListResponse<T> {
        private List<T> bookings;
        private List<T> reservations; // 추가: 숙소 및 렌터카를 위한 필드
        private long totalCount;
        private int page;
        private int size;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageFlightBookingResponse {
        private Long bookingId;
        private String bookingCode;
        private String flightNumber;
        private String origin;
        private String destination;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private SeatClass seatClass;
        private String passengerName;
        private BigDecimal totalPrice;
        private BookingStatus status;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageInsurancePolicyResponse {
        private Long policyId;
        private String policyCode;
        private String productName;
        private String insuredName;
        private String startDate;
        private String endDate;
        private String coverageLevel;
        private BigDecimal totalPremium;
        private String status;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageRoomReservationResponse {
        private Long reservationId;
        private String accommodationName;
        private String roomName;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private BigDecimal totalPrice;
        private String status;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageCarReservationResponse {
        private Long reservationId;
        private String modelName;
        private String carType;
        private String checkIn;
        private String checkOut;
        private BigDecimal totalPrice;
        private String status;
    }
}
