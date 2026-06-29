package com.onde.admin.application.booking.dto;

//검색된 예약 목록(List)과 총 데이터 개수를 묶어서 프론트엔드로 보내줍니다.
//경로: admin-module/src/main/java/com/onde/admin/application/booking/dto/AdminBookingSearchResponse.java

import java.util.List;

public record AdminBookingSearchResponse(
    List<AdminBookingDto> bookings,
    long totalCount
) {}