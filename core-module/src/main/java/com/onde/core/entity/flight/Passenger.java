package com.onde.core.entity.flight;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Passenger {

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_passport", nullable = false, length = 50)
    private String passengerPassport;

    @Column(name = "passenger_birthdate", nullable = false)
    private LocalDate passengerBirthdate;
}
