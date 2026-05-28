package com.onde.core.entity.insurance;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code", unique = true, nullable = false, length = 30)
    private String policyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_product_id", foreignKey = @ForeignKey(name = "fk_policy_product"))
    private InsuranceProduct insuranceProduct;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "insured_name", nullable = false, length = 100)
    private String insuredName;

    @Column(name = "insured_birthdate", nullable = false)
    private LocalDate insuredBirthdate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "coverage_level", nullable = false, length = 20)
    private String coverageLevel; // STANDARD, DELUXE, PREMIUM

    @Column(name = "total_premium", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPremium;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED
}
