package com.onde.core.repository;

import com.onde.core.entity.flight.SeatClass;
import com.onde.core.entity.flight.SeatInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {
    List<SeatInventory> findByFlightScheduleIdIn(List<Long> flightScheduleIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInventory si WHERE si.flightScheduleId = :scheduleId AND si.classType = :classType")
    Optional<SeatInventory> findWithLockByFlightScheduleIdAndClassType(
        @Param("scheduleId") Long scheduleId,
        @Param("classType") SeatClass classType
    );
}
