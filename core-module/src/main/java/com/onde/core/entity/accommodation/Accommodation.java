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

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "business_license", nullable = false, length = 100)
    private String businessLicense;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "submit_date", nullable = false)
    private LocalDateTime submitDate;
}
