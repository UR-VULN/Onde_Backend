package com.onde.core.entity.accommodation;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "accommodations")
@Getter @Setter @NoArgsConstructor
public class Accommodation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodation_id")
    private Long id;

    @Column(name = "seller_id")
    private Long sellerId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String location;

    private Double rating;

    @Column(name = "business_license")
    private String businessLicense;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "submit_date")
    private LocalDateTime submitDate;
}
