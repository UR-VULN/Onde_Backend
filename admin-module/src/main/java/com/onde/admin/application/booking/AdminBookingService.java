package com.onde.admin.application.booking;

import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBookingService {
    private final ReservationRepository reservationRepository;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional
    public void completeReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reservation.setStatus(ReservationStatus.COMPLETED);
    }
}
