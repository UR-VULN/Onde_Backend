package com.onde.admin.application.booking.dto;

/*조회된 예약 목록에서 각각의 예약 건을 보여줄 데이터 규격입니다.
경로: admin-module/src/main/java/com/onde/admin/application/booking/dto/AdminBookingDto.java
*/

import com.onde.core.entity.reservation.ReservationStatus;
import java.time.LocalDateTime;

public record AdminBookingDto(
    Long reservationId,
    String memberName,
    String targetName, // 숙소명 또는 렌터카 모델명
    LocalDateTime checkInDate,
    LocalDateTime checkOutDate,
    ReservationStatus status
) {}