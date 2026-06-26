package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "scheduleId는 필수입니다.")
    @Min(value = 1, message = "scheduleId 형식이 올바르지 않습니다.")
    private Long scheduleId;

    @NotNull(message = "좌석 등급은 필수입니다.")
    private SeatClass seatClass;

    @Valid
    @Size(max = ValidationLimits.PASSENGER_MAX, message = "승객 수는 9명 이하여야 합니다.")
    private List<PassengerDto> passengers;

    @Size(max = ValidationLimits.NAME_MAX, message = "승객명은 100자 이하여야 합니다.")
    private String passengerName;

    @Size(max = ValidationLimits.PASSPORT_MAX, message = "여권번호는 20자 이하여야 합니다.")
    private String passengerPassport;

    private LocalDate passengerBirthdate;

    @DecimalMin(value = "0", message = "총 금액은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "총 금액이 허용 범위를 초과합니다.")
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

        @Size(max = ValidationLimits.NAME_MAX, message = "승객명은 100자 이하여야 합니다.")
        private String name;

        @Size(max = ValidationLimits.PASSPORT_MAX, message = "여권번호는 20자 이하여야 합니다.")
        private String passportNumber;

        private LocalDate birthdate;
    }
}
