package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import jakarta.validation.constraints.*;
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
    @NotNull(message = "스케줄 ID는 필수입니다.")
    private Long scheduleId;

    @NotNull(message = "좌석 등급은 필수입니다.")
    private SeatClass seatClass;

    private List<PassengerDto> passengers;

    @Size(max = 100, message = "이름은 최대 100자까지 가능합니다.")
    private String passengerName;

    @Size(max = 20, message = "여권 번호는 최대 20자까지 가능합니다.")
    private String passengerPassport;

    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate passengerBirthdate;

    @DecimalMin(value = "0.0", inclusive = false, message = "총 가격은 0보다 커야 합니다.")
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
        @NotBlank(message = "승객 이름은 필수입니다.")
        private String name;
        @NotBlank(message = "여권 번호는 필수입니다.")
        private String passportNumber;
        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        private LocalDate birthdate;
    }
}
