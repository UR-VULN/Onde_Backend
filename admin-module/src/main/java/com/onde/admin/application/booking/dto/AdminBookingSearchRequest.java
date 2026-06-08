package com.onde.admin.application.booking.dto;

/*
관리자가 예약을 검색할 때 보낼 조건들(날짜, 상태 등)을 담는 그릇입니다.
경로: admin-module/src/main/java/com/onde/admin/application/booking/dto/AdminBookingSearchRequest.java
*/


import com.onde.core.entity.reservation.ReservationStatus;
import java.time.LocalDate;

public record AdminBookingSearchRequest(
    String targetType,
    ReservationStatus status,
    String memberName,
    LocalDate startDate,
    LocalDate endDate
) {}