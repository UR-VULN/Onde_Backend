package com.onde.core.repository;

import com.onde.core.entity.accommodation.CarInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarInventoryRepository extends JpaRepository<CarInventory, Long> {
    List<CarInventory> findByCar_CarIdAndInventoryDateBetween(Long carId, LocalDate startDate, LocalDate endDate);
    Optional<CarInventory> findByCar_CarIdAndInventoryDate(Long carId, LocalDate inventoryDate);
}
