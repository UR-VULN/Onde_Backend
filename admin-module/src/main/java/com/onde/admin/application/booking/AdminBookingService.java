package com.onde.admin.application.booking;

/*예약 내역을 조회하고, 상태를 '이용 완료'로 업데이트하는 실제 서비스 코드입니다.
경로: admin-module/src/main/java/com/onde/admin/application/booking/AdminBookingService.java
*/

import com.onde.admin.application.booking.dto.AdminBookingDto;
import com.onde.admin.application.booking.dto.AdminBookingSearchRequest;
import com.onde.admin.application.booking.dto.AdminBookingSearchResponse;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final ReservationRepository reservationRepository;

    /**
     * 투숙객/이용자 명단 조회
     */
    @Transactional(readOnly = true)
    public AdminBookingSearchResponse searchBookings(AdminBookingSearchRequest request) {
        
        // 실무에서는 QueryDSL 등을 사용해 필터링하지만, 우선 동작 확인을 위해 전체 조회를 매핑합니다.
        List<Reservation> reservations = reservationRepository.findAll();
        
        List<AdminBookingDto> dtos = reservations.stream()
                .map(r -> new AdminBookingDto(
                        r.getId(), // Reservation 엔터티의 ID getter (예: getReservationId() 라면 수정 필요)
                        "예약자명", // 실제: r.getMember().getName() 등으로 수정
                        "숙소/렌터카명", // 실제: r.getTargetName() 등으로 수정
                        null, // 실제: 체크인 시간 (r.getCheckIn() 등)
                        null, // 실제: 체크아웃 시간 (r.getCheckOut() 등)
                        r.getStatus() // 실제: 예약 상태 getter
                ))
                .collect(Collectors.toList());

        return new AdminBookingSearchResponse(dtos, dtos.size());
    }

    /**
     * 이용 완료 상태 강제 업데이트
     */
    @Transactional
    public void forceCompleteBooking(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        // 예약 상태를 COMPLETED(이용 완료)로 강제 변경합니다.
        reservation.setStatus(ReservationStatus.COMPLETED); 
    }
}