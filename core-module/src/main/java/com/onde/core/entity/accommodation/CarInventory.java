package com.onde.core.entity.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
public class CarInventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CarId")
    private Car car;

    private LocalDate inventoryDate;
    private Integer availableQuantity;
    private Integer price;
}
