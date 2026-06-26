package com.onde.admin.application.booking;

import com.onde.admin.application.reservation.dto.AdminReservationCancelResponse;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateRequest;
import com.onde.admin.application.reservation.dto.AdminReservationStatusUpdateResponse;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.FlightBookingRepository;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.core.repository.RoomRepository;
import com.onde.core.repository.SeatInventoryRepository;
import com.onde.core.security.PassportFieldCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceReservationTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private FlightBookingRepository flightBookingRepository;

    @Mock
    private SeatInventoryRepository seatInventoryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PassportFieldCodec passportFieldCodec;

    @InjectMocks
    private AdminBookingService adminBookingService;

    @Test
    void updateReservationStatusChangesCommonReservationStatus() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        AdminReservationStatusUpdateResponse response = adminBookingService.updateReservationStatus(
                10L,
                new AdminReservationStatusUpdateRequest("COMPLETED", "E2E")
        );

        assertEquals(ReservationStatus.RESERVED, response.previousStatus());
        assertEquals(ReservationStatus.COMPLETED, response.currentStatus());
        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
    }

    @Test
    void cancelReservationByAdminCancelsCommonReservation() {
        Reservation reservation = Reservation.builder()
                .id(11L)
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findById(11L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        AdminReservationCancelResponse response = adminBookingService.cancelReservationByAdmin(11L);

        assertEquals(ReservationStatus.CANCELLED, response.status());
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }
}
