package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.RoomInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerAccommodationRegisterRequest;
import com.onde.core.entity.accommodation.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seller/accommodations")
@RequiredArgsConstructor
public class SellerAccommodationController {
    private final SellerAccommodationService sellerAccommodationService;

    @PostMapping
    public ResponseEntity<Long> register(@RequestBody SellerAccommodationRegisterRequest request) {
        return ResponseEntity.ok(sellerAccommodationService.registerAccommodation(request));
    }

    @GetMapping("/rooms/{roomId}/inventory")
    public ResponseEntity<List<Inventory>> getInventories(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(sellerAccommodationService.getRoomInventories(roomId, startDate, endDate));
    }

    @PutMapping("/rooms/{roomId}/inventory")
    public ResponseEntity<Void> updateInventories(
            @PathVariable Long roomId,
            @RequestBody List<RoomInventoryUpdateRequest> requests) {
        sellerAccommodationService.updateRoomInventories(roomId, requests);
        return ResponseEntity.ok().build();
    }
}
