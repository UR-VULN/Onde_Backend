package com.onde.core.entity.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Accommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccommodationId")
    private Long accommodationId;

    @Column(name = "SellerId")
    private Long sellerId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description; 

    private String region;
    private String city;
    private Integer starRating;

    private Double latitude;
    private Double longitude;

    @ElementCollection
    @CollectionTable(name = "accommodation_amenities", joinColumns = @JoinColumn(name = "AccommodationId"))
    @Column(name = "amenity")
    private List<String> amenities;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;
}
