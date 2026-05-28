package com.onde.api.application.accommodation;

import com.onde.api.application.accommodation.dto.AccommodationListDto;
import com.onde.api.application.accommodation.dto.AccommodationSearchRequest;
import com.onde.api.application.accommodation.dto.AccommodationSearchResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;

    public AccommodationSearchResponse searchAccommodations(AccommodationSearchRequest request) {
        Long days = null;
        if (request.getCheckIn() != null && request.getCheckOut() != null) {
            days = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if ("price_asc".equals(request.getSort())) {
            // Note: Sorting by min price across rooms/inventories is complex in JPA Sort.
            // For now, we will sort by id as a placeholder or use rating.
        } else if ("price_desc".equals(request.getSort())) {
            // Placeholder
        } else if ("rating".equals(request.getSort())) {
            sort = Sort.by(Sort.Direction.DESC, "rating");
        }

        List<Accommodation> accommodations = accommodationRepository.searchAccommodations(
                ApprovalStatus.APPROVED, 
                request.getRegion(), 
                request.getCategory(),
                request.getCheckIn(),
                request.getCheckOut() != null ? request.getCheckOut().minusDays(1) : null,
                days,
                sort);

        List<AccommodationListDto> listDtos = accommodations.stream()
                .map(a -> AccommodationListDto.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .category(a.getCategory())
                        .location(a.getLocation())
                        .thumbnailUrl(a.getThumbnailUrl())
                        .rating(a.getRating())
                        .minPrice(100000) // Placeholder
                        .build())
                .collect(Collectors.toList());

        return AccommodationSearchResponse.builder()
                .accommodations(listDtos)
                .build();
    }
}
