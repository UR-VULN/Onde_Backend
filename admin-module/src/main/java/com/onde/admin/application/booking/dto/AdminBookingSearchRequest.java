package com.onde.admin.application.booking.dto;

/*
관리자가 예약을 검색할 때 보낼 조건들(날짜, 상태 등)을 담는 그릇입니다.
경로: admin-module/src/main/java/com/onde/admin/application/booking/dto/AdminBookingSearchRequest.java
*/


import com.onde.core.entity.reservation.ReservationStatus;
import java.time.LocalDate;

public record AdminBookingSearchRequest(
    String targetType, // ACCOMMODATION (숙소) 또는 CAR (렌터카)
    ReservationStatus status, // 예약 상태 필터
    String memberName, // 예약자명 검색
    LocalDate startDate,
    LocalDate endDate
) {}