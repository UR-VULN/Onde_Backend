package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerAccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 1.7. 판매자 숙소 신규 등록 비즈니스 로직 (주소 규격 포함)
     * 신규 숙소 엔티티를 검수 대기(PENDING) 상태로 생성하고, 
     * 요청 정보에 포함된 객실(Room) 목록도 연관 매핑하여 저장합니다.
     *
     * @param request 신규 등록할 숙소 정보와 객실 정보 리스트 DTO
     * @return 등록 완료된 숙소 ID
     */
    @Transactional
    public Long registerAccommodation(SellerAccommodationRegisterRequest request) {
        Accommodation accommodation = new Accommodation();
        accommodation.setSellerId(request.getSellerId());
        accommodation.setName(request.getName());
        accommodation.setDescription(request.getDescription());
        accommodation.setCategory(request.getCategory());
        accommodation.setLocation(request.getLocation());
        accommodation.setBusinessLicense(request.getBusinessLicense());
        accommodation.setThumbnailUrl(request.getThumbnailUrl());
        accommodation.setApprovalStatus(ApprovalStatus.PENDING);
        accommodation.setSubmitDate(LocalDateTime.now());

        Accommodation savedAccommodation = accommodationRepository.save(accommodation);

        for (SellerAccommodationRegisterRequest.RoomRegisterRequest roomReq : request.getRooms()) {
            Room room = new Room();
            room.setAccommodation(savedAccommodation);
            room.setName(roomReq.getName());
            room.setCapacity(roomReq.getCapacity());
            roomRepository.save(room);
        }

        return savedAccommodation.getId();
    }

    public List<Inventory> getRoomInventories(Long roomId, LocalDate startDate, LocalDate endDate) {
        return inventoryRepository.findByTargetTypeAndTargetIdAndDateBetween(
                ReservationTarget.ROOM, roomId, startDate, endDate);
    }

    @Transactional
    public void updateRoomInventories(Long roomId, List<RoomInventoryUpdateRequest> requests) {
        for (RoomInventoryUpdateRequest request : requests) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findByTargetTypeAndTargetIdAndDate(
                    ReservationTarget.ROOM, roomId, request.getDate());

            Inventory inventory;
            if (inventoryOpt.isPresent()) {
                inventory = inventoryOpt.get();
            } else {
                inventory = new Inventory();
                inventory.setTargetType(ReservationTarget.ROOM);
                inventory.setTargetId(roomId);
                inventory.setDate(request.getDate());
            }

            if (request.getStock() != null) {
                inventory.setStock(request.getStock());
            }
            if (request.getBasePrice() != null) {
                inventory.setBasePrice(request.getBasePrice());
            }

            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public void updateRoomInventoriesBulk(List<RoomInventoryUpdateRequest> requests) {
        for (RoomInventoryUpdateRequest request : requests) {
            if (request.getRoomId() == null)
                continue;

            Optional<Inventory> inventoryOpt = inventoryRepository.findByTargetTypeAndTargetIdAndDate(
                    ReservationTarget.ROOM, request.getRoomId(), request.getDate());

            Inventory inventory;
            if (inventoryOpt.isPresent()) {
                inventory = inventoryOpt.get();
            } else {
                inventory = new Inventory();
                inventory.setTargetType(ReservationTarget.ROOM);
                inventory.setTargetId(request.getRoomId());
                inventory.setDate(request.getDate());
            }

            if (request.getStock() != null) {
                inventory.setStock(request.getStock());
            }
            if (request.getBasePrice() != null) {
                inventory.setBasePrice(request.getBasePrice());
            }

            inventoryRepository.save(inventory);
        }
    }

    /**
     * 1.7. 판매자 등록 숙소 목록 조회 비즈니스 로직
     * 특정 판매자(Seller ID)가 등록한 모든 숙소 엔티티 목록을 조회합니다.
     *
     * @param sellerId 판매자 고유 식별 ID
     * @return 판매자 소유의 숙소 엔티티 리스트
     */
    public List<Accommodation> getAccommodations(Long sellerId) {
        return accommodationRepository.findBySellerId(sellerId);
    }
}
