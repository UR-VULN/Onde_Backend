package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationListDto;
import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;

    public AccommodationSearchResponse searchAccommodations(AccommodationSearchRequest request) {
        List<Accommodation> accommodations = accommodationRepository.searchAccommodations(
                ApprovalStatus.APPROVED, request.getRegion(), request.getStarRating());

        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            accommodations = accommodations.stream()
                    .filter(a -> a.getAmenities().containsAll(request.getAmenities()))
                    .toList();
        }

        List<AccommodationListDto> listDtos = accommodations.stream()
                .map(a -> AccommodationListDto.builder()
                        .accommodationId(a.getAccommodationId())
                        .name(a.getName())
                        .region(a.getRegion())
                        .city(a.getCity())
                        .starRating(a.getStarRating())
                        .amenities(a.getAmenities())
                        .minPrice(100000) // Placeholder
                        .build())
                .toList();

        return AccommodationSearchResponse.builder()
                .accommodations(listDtos)
                .build();
    }
}
