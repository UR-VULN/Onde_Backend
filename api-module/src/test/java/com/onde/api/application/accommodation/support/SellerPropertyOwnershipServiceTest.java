package com.onde.api.application.accommodation.support;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerPropertyOwnershipServiceTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private SellerPropertyOwnershipService ownershipService;

    @Test
    void allowsOwnerForStayProperty() {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(10L);
        when(accommodationRepository.findById(1L)).thenReturn(Optional.of(accommodation));

        assertDoesNotThrow(() -> ownershipService.assertSellerOwnsProperty(10L, "stay-1"));
    }

    @Test
    void rejectsNonOwnerForStayProperty() {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(10L);
        when(accommodationRepository.findById(1L)).thenReturn(Optional.of(accommodation));

        assertThrows(ForbiddenException.class, () -> ownershipService.assertSellerOwnsProperty(99L, "stay-1"));
    }

    @Test
    void allowsOwnerForCarProperty() {
        Car car = new Car();
        car.setSellerId(20L);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));

        assertDoesNotThrow(() -> ownershipService.assertSellerOwnsProperty(20L, "car-5"));
    }

    @Test
    void rejectsNonOwnerForCarProperty() {
        Car car = new Car();
        car.setSellerId(20L);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));

        assertThrows(ForbiddenException.class, () -> ownershipService.assertSellerOwnsProperty(77L, "car-5"));
    }

    @Test
    void allowsOwnerForRoomId() {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(10L);
        Room room = new Room();
        room.setAccommodation(accommodation);
        when(accommodationRepository.findById(3L)).thenReturn(Optional.empty());
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room));

        assertDoesNotThrow(() -> ownershipService.assertSellerOwnsRoom(10L, 3L));
    }

    @Test
    void rejectsNonOwnerForRoomId() {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(10L);
        Room room = new Room();
        room.setAccommodation(accommodation);
        when(accommodationRepository.findById(3L)).thenReturn(Optional.empty());
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room));

        assertThrows(ForbiddenException.class, () -> ownershipService.assertSellerOwnsRoom(99L, 3L));
    }
}
