package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FlightBookingRequest {
    private Long scheduleId;
    private SeatClass seatClass;
    private List<PassengerDto> passengers;
    private String passengerName;
    private String passengerPassport;
    private LocalDate passengerBirthdate;
    private BigDecimal totalPrice;

    public String getPassengerName() {
        return passengerName != null ? passengerName : firstPassengerName();
    }

    public String getPassengerPassport() {
        if (passengerPassport != null) {
            return passengerPassport;
        }
        return passengers != null && !passengers.isEmpty() ? passengers.get(0).getPassportNumber() : null;
    }

    public LocalDate getPassengerBirthdate() {
        if (passengerBirthdate != null) {
            return passengerBirthdate;
        }
        return passengers != null && !passengers.isEmpty() ? passengers.get(0).getBirthdate() : null;
    }

    public int passengerCount() {
        return passengers != null && !passengers.isEmpty() ? passengers.size() : 1;
    }

    private String firstPassengerName() {
        return passengers != null && !passengers.isEmpty() ? passengers.get(0).getName() : null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PassengerDto {
        private String name;
        private String passportNumber;
        private LocalDate birthdate;
    }
}
