package com.onde.api.application.accommodation.support;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 inventory API propertyKey 소유권 검증.
 * stay-{id}: 숙소(accommodation) ID 또는 객실(room) ID
 * car-{id}: 렌터카 ID
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerPropertyOwnershipService {

    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final CarRepository carRepository;

    public void assertSellerOwnsRoom(Long sellerMemberId, Long roomId) {
        if (sellerMemberId == null) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        Long ownerId = resolveStayOwnerId(roomId);
        if (!sellerMemberId.equals(ownerId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
    }

    public void assertSellerOwnsProperty(Long sellerMemberId, String propertyKey) {
        if (sellerMemberId == null) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        String lower = propertyKey.toLowerCase();
        if (lower.startsWith("stay") || lower.startsWith("room")) {
            Long targetId = parseTargetId(propertyKey);
            Long ownerId = resolveStayOwnerId(targetId);
            if (!sellerMemberId.equals(ownerId)) {
                throw new ForbiddenException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        if (lower.startsWith("car")) {
            Long carId = parseTargetId(propertyKey);
            Car car = carRepository.findById(carId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.CAR_NOT_FOUND));
            if (!sellerMemberId.equals(car.getSellerId())) {
                throw new ForbiddenException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        throw new IllegalArgumentException("Unknown propertyKey prefix: " + propertyKey);
    }

    private Long resolveStayOwnerId(Long targetId) {
        return accommodationRepository.findById(targetId)
                .map(Accommodation::getSellerId)
                .orElseGet(() -> roomRepository.findById(targetId)
                        .map(Room::getAccommodation)
                        .map(Accommodation::getSellerId)
                        .orElseThrow(() -> new NotFoundException(ErrorCode.ROOM_NOT_FOUND)));
    }

    private Long parseTargetId(String propertyKey) {
        if (propertyKey == null || !propertyKey.contains("-")) {
            throw new IllegalArgumentException("Invalid propertyKey format");
        }
        return Long.parseLong(propertyKey.split("-")[1]);
    }
}
