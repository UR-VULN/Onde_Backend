package com.onde.admin.application.approval;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {
    private final AccommodationRepository accommodationRepository;
    private final CarRepository carRepository;

    public List<Accommodation> getPendingAccommodations() {
        // Simple implementation: fetch all and filter or add custom query to repo
        return accommodationRepository.findAll().stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.PENDING)
                .toList();
    }

    @Transactional
    public void approveAccommodation(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new RuntimeException("Accommodation not found"));
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Transactional
    public void rejectAccommodation(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new RuntimeException("Accommodation not found"));
        accommodation.setApprovalStatus(ApprovalStatus.REJECTED);
    }

    public List<Car> getPendingCars() {
        return carRepository.findAll().stream()
                .filter(c -> c.getApprovalStatus() == ApprovalStatus.PENDING)
                .toList();
    }

    @Transactional
    public void approveCar(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        car.setApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Transactional
    public void rejectCar(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        car.setApprovalStatus(ApprovalStatus.REJECTED);
    }
}
