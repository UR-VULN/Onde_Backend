package com.onde.core.repository;

import com.onde.core.entity.payment.Payment;
import com.onde.core.entity.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * PG사 결제 완료 후, 가맹점 주문번호(merchantUid)로 결제 건을 조회합니다.
     * 포트원 웹훅 수신 및 결제 사후 검증 시 결제 내역 확인을 위해 사용됩니다.
     *
     * @param merchantUid 가맹점에서 발급한 고유 주문번호
     * @return 조회된 결제 정보 (존재할 경우)
     */
    Optional<Payment> findByMerchantUid(String merchantUid);

    /**
     * 지정된 기간 동안 특정 결제 상태를 가진 거래를 대상으로 판매자별 총 정산 대상 금액(수수료 차감 전)을 조회합니다.
     * 월별 정산 데이터 생성을 위해 결제 테이블과 예약 테이블을 조인하여 판매자별 grossAmount의 합계를 집계합니다.
     *
     * @param status 집계할 결제 상태 (일반적으로 PAID)
     * @param start  집계 대상 시작 일시 (포함)
     * @param end    집계 대상 종료 일시 (미포함)
     * @return 판매자 식별자 및 총 매출액을 담은 프로젝션 목록
     */
    @Query("SELECT r.sellerId AS sellerId, " +
            "SUM(p.totalAmount) AS grossAmount " +
            "FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE p.status = :status " +
            "AND p.createdAt >= :start " +
            "AND p.createdAt < :end " +
            "GROUP BY r.sellerId")
    List<SettlementProjection> calculateSettlementAmounts(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 정산 금액 집계를 위한 Spring Data JPA 프로젝션 인터페이스입니다.
     */
    interface SettlementProjection {
        /**
         * @return 판매자 식별자
         */
        Long getSellerId();

        /**
         * @return 판매자의 총 매출액 (수수료 차감 전)
         */
        Long getGrossAmount();
    }

    /**
     * 특정 결제 상태인 전체 결제 건의 총거래액(GMV)과 수수료 순이익(매출의 3%)을 합산하여 조회합니다.
     *
     * @param status 집계할 결제 상태 (일반적으로 PAID)
     * @return 플랫폼 전체 총거래액 및 수수료 금액 프로젝션
     */
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) AS totalGmv, " +
            "COALESCE(CAST(SUM(p.totalAmount) * 0.03 AS Long), 0) AS totalCommission " +
            "FROM Payment p WHERE p.status = :status")
    PlatformRevenueProjection calculatePlatformRevenue(@Param("status") PaymentStatus status);

    /**
     * 특정 결제 상태인 결제 건들의 서비스 종류(reservationType)별 총거래액을 집계하여 조회합니다.
     *
     * @param status 집계할 결제 상태 (일반적으로 PAID)
     * @return 서비스 분류별 총거래액 목록
     */
    @Query("SELECT p.reservationType AS serviceType, " +
            "SUM(p.totalAmount) AS serviceGmv " +
            "FROM Payment p " +
            "WHERE p.status = :status " +
            "AND p.reservationType IS NOT NULL " +
            "GROUP BY p.reservationType")
    List<ServiceRevenueProjection> calculateRevenueByService(@Param("status") PaymentStatus status);

    /**
     * 플랫폼 전체 매출 및 수수료 집계를 위한 프로젝션 인터페이스입니다.
     */
    interface PlatformRevenueProjection {
        Long getTotalGmv();
        Long getTotalCommission();
    }

    /**
     * 서비스별 매출 집계를 위한 프로젝션 인터페이스입니다.
     */
    interface ServiceRevenueProjection {
        String getServiceType();
        Long getServiceGmv();
    }
}

