package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.accommodation.RoomInventory;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.RoomInventoryRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerAccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final RoomInventoryRepository roomInventoryRepository;

    @Transactional
    public Long registerAccommodation(SellerAccommodationRegisterRequest request) {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(request.getSellerId());
        accommodation.setName(request.getName());
        accommodation.setDescription(request.getDescription());
        accommodation.setRegion(request.getRegion());
        accommodation.setCity(request.getCity());
        accommodation.setStarRating(request.getStarRating());
        accommodation.setLatitude(request.getLatitude());
        accommodation.setLongitude(request.getLongitude());
        accommodation.setAmenities(request.getAmenities());
        accommodation.setApprovalStatus(ApprovalStatus.PENDING);

        Accommodation savedAccommodation = accommodationRepository.save(accommodation);

        for (SellerAccommodationRegisterRequest.RoomRegisterRequest roomReq : request.getRooms()) {
            Room room = new Room();
            room.setAccommodation(savedAccommodation);
            room.setRoomType(roomReq.getRoomType());
            room.setBaseCapacity(roomReq.getBaseCapacity());
            room.setMaxCapacity(roomReq.getMaxCapacity());
            room.setDefaultPrice(roomReq.getDefaultPrice());
            room.setTotalQuantity(roomReq.getTotalQuantity());
            roomRepository.save(room);
        }

        return savedAccommodation.getAccommodationId();
    }

    public List<RoomInventory> getRoomInventories(Long roomId, LocalDate startDate, LocalDate endDate) {
        return roomInventoryRepository.findByRoom_RoomIdAndInventoryDateBetween(roomId, startDate, endDate);
    }

    @Transactional
    public void updateRoomInventories(Long roomId, List<RoomInventoryUpdateRequest> requests) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        for (RoomInventoryUpdateRequest request : requests) {
            Optional<RoomInventory> inventoryOpt = roomInventoryRepository.findByRoom_RoomIdAndInventoryDate(
                    roomId, request.getDate());

            RoomInventory inventory;
            if (inventoryOpt.isPresent()) {
                inventory = inventoryOpt.get();
            } else {
                inventory = new RoomInventory();
                inventory.setRoom(room);
                inventory.setInventoryDate(request.getDate());
            }

            if (request.getAvailableQuantity() != null) {
                inventory.setAvailableQuantity(request.getAvailableQuantity());
            }
            if (request.getPrice() != null) {
                inventory.setPrice(request.getPrice());
            }

            roomInventoryRepository.save(inventory);
        }
    }
}
