package com.onde.core.entity.insurance;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.flight.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "insurance_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "base_daily_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal baseDailyRate;

    @Column(name = "coverage_details", columnDefinition = "json", nullable = false)
    private String coverageDetails; // MySQL JSON Type mapping

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING_APPROVAL'")
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING_APPROVAL;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;
}
