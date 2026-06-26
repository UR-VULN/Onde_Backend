package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.SellerCarInventoryUpdateRequest;
import com.onde.api.application.accommodation.dto.SellerCarMultipartForm;
import com.onde.core.validation.MultipartInputValidator;
import com.onde.api.application.accommodation.dto.SellerCarRegisterRequest;
import com.onde.api.security.LoginMember;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.repository.CarRepository;
import com.onde.core.repository.InventoryRepository;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerCarController {

    private final CarService carService;
    private final CarRepository carRepository;
    private final InventoryRepository inventoryRepository;
    private final com.onde.api.config.MockS3Uploader s3Uploader;

    @GetMapping("/cars")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCars(
            @LoginMember Long sellerId) {
        List<com.onde.core.entity.accommodation.Car> list = carService.getCarsBySellerId(sellerId);
        List<Map<String, Object>> mapped = list.stream().map(c -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("propertyId", c.getId());
            item.put("name", c.getModelName());
            String status = "ACTIVE";
            if (c.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.PENDING) {
                status = "PENDING";
            } else if (c.getApprovalStatus() == com.onde.core.entity.accommodation.ApprovalStatus.REJECTED) {
                status = "REJECTED";
            }
            item.put("status", status);
            item.put("basePrice", 50000);
            return item;
        }).toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("cars", mapped);
        data.put("totalCount", mapped.size());

        return ResponseEntity.ok(ApiResponse.success(data, "판매자 등록 렌터카 목록 조회가 성공적으로 완료되었습니다."));
    }

    @PostMapping(value = "/cars", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> registerCarMultipart(
            @RequestParam(required = false) MultipartFile thumbnail,
            @Valid @ModelAttribute SellerCarMultipartForm form,
            @LoginMember Long sellerId) {
        MultipartInputValidator.validateOptionalImage(thumbnail);

        if (carRepository.existsByLicensePlate(form.getLicensePlate())) {
            throw new IllegalArgumentException("이미 등록된 차량 번호입니다.");
        }

        Car car = new Car();
        car.setSellerId(sellerId);
        car.setModelName(form.getModelName());
        car.setCarType(form.getCarType());
        car.setLicensePlate(form.getLicensePlate());
        car.setLocation(form.getLocation() == null || form.getLocation().isBlank() ? "제주" : form.getLocation());
        car.setApprovalStatus(com.onde.core.entity.accommodation.ApprovalStatus.PENDING);
        if (thumbnail != null && !thumbnail.isEmpty()) {
            car.setThumbnailUrl(s3Uploader.upload(thumbnail, "cars"));
        }
        Car saved = carRepository.save(car);

        String dailyPrice = form.getDailyPrice();
        if (dailyPrice != null && !dailyPrice.isBlank()) {
            BigDecimal priceVal = new BigDecimal(dailyPrice);
            Inventory inventory = new Inventory();
            inventory.setTargetType(ReservationTarget.CAR);
            inventory.setTargetId(saved.getId());
            inventory.setDate(LocalDate.now());
            inventory.setBasePrice(priceVal);
            inventory.setStock(1);
            inventoryRepository.save(inventory);
        }

        return ResponseEntity.ok(ApiResponse.success(saved.getId(), "렌터카 등록 신청이 완료되었습니다."));
    }

    @PostMapping("/cars")
    public ResponseEntity<ApiResponse<Long>> registerCar(
            @Valid @RequestBody SellerCarRegisterRequest request,
            @LoginMember Long sellerId) {
        if (carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("이미 등록된 차량 번호입니다.");
        }

        Car car = new Car();
        car.setSellerId(sellerId);
        car.setModelName(request.getModelName());
        car.setCarType(request.getCarType());
        car.setLicensePlate(request.getLicensePlate());
        car.setApprovalStatus(com.onde.core.entity.accommodation.ApprovalStatus.PENDING);

        if (request.getThumbnailUrl() != null) {
            car.setThumbnailUrl(request.getThumbnailUrl());
        }

        Car saved = carRepository.save(car);

        if (request.getDailyPrice() != null) {
            Inventory inventory = new Inventory();
            inventory.setTargetType(ReservationTarget.CAR);
            inventory.setTargetId(saved.getId());
            inventory.setDate(LocalDate.now());
            inventory.setBasePrice(BigDecimal.valueOf(request.getDailyPrice()));
            inventory.setStock(1);
            inventoryRepository.save(inventory);
        }

        return ResponseEntity.ok(ApiResponse.success(saved.getId(), "렌터카 등록 신청이 완료되었습니다."));
    }

    @PutMapping("/inventories/cars")
    public ResponseEntity<ApiResponse<Void>> updateCarInventory(
            @Valid @RequestBody SellerCarInventoryUpdateRequest request,
            @LoginMember Long sellerId) {
        Long carId = request.getCarId();
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("carId가 존재하지 않습니다."));
        if (!car.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("본인 소유 차량만 수정할 수 있습니다.");
        }

        Inventory inventory = inventoryRepository.findByTargetTypeAndTargetIdAndDate(
                        ReservationTarget.CAR, carId, LocalDate.now())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setTargetType(ReservationTarget.CAR);
                    created.setTargetId(carId);
                    created.setDate(LocalDate.now());
                    created.setBasePrice(BigDecimal.ZERO);
                    created.setStock(0);
                    return created;
                });
        if (request.getDailyPrice() != null) {
            inventory.setBasePrice(request.getDailyPrice());
        }
        if (request.getAvailableCount() != null) {
            inventory.setStock(request.getAvailableCount());
        }
        inventoryRepository.save(inventory);
        return ResponseEntity.ok(ApiResponse.success(null, "렌터카 재고 및 가격이 수정되었습니다."));
    }
}
