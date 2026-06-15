package com.onde.core.repository;

import com.onde.core.entity.payment.Payment;
import com.onde.core.entity.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 결제 완료 후, 가맹점 주문번호(merchantUid)로 결제 건을 조회합니다.
     * 결제 사후 검증 시 결제 내역 확인을 위해 사용됩니다.
     *
     * @param merchantUid 가맹점에서 발급한 고유 주문번호
     * @return 조회된 결제 정보 (존재할 경우)
     */
    Optional<Payment> findByMerchantUid(String merchantUid);
    
    /**
     * 예약 식별자(reservationId)로 결제 내역을 조회합니다.
     */
    Optional<Payment> findByReservationId(Long reservationId);

    List<Payment> findBySettlementId(Long settlementId);

    /**
     * 예약 식별자(reservationId)로 가장 최근의 결제 내역 1건을 조회합니다.
     */
    Optional<Payment> findFirstByReservationIdOrderByIdDesc(Long reservationId);

    /**
     * 예약 식별자와 예약 타입으로 가장 최근의 결제 내역 1건을 조회합니다.
     */
    Optional<Payment> findFirstByReservationIdAndReservationTypeOrderByIdDesc(Long reservationId, String reservationType);


    /**
     * 지정된 기간 동안 특정 결제 상태를 가진 거래를 대상으로 판매자별 총 정산 대상 금액(수수료 차감 전)을 조회합니다.
     * 월별 정산 데이터 생성을 위해 결제 테이블과 예약 테이블을 조인하여 판매자별 grossAmount의 합계를 집계합니다.
     *
     * @param status 집계할 결제 상태 (일반적으로 PAID)
     * @param start  집계 대상 시작 일시 (포함)
     * @param end    집계 대상 종료 일시 (미포함)
     * @return 판매자 식별자 및 총 매출액을 담은 프로젝션 목록
     */
    @Query(value = "SELECT " +
            "CASE " +
            "  WHEN r.target_type = 'ROOM' THEN a.seller_id " +
            "  WHEN r.target_type = 'CAR' THEN c.seller_id " +
            "  ELSE null " +
            "END AS sellerId, " +
            "SUM(p.total_amount) AS grossAmount " +
            "FROM payments p " +
            "JOIN reservations r ON p.reservation_id = r.id " +
            "LEFT JOIN rooms rm ON r.target_type = 'ROOM' AND r.target_id = rm.id " +
            "LEFT JOIN accommodations a ON rm.accommodation_id = a.id " +
            "LEFT JOIN rental_cars c ON r.target_type = 'CAR' AND r.target_id = c.id " +
            "WHERE p.status = :status " +
            "AND p.settlement_id IS NULL " +
            "GROUP BY " +
            "CASE " +
            "  WHEN r.target_type = 'ROOM' THEN a.seller_id " +
            "  WHEN r.target_type = 'CAR' THEN c.seller_id " +
            "  ELSE null " +
            "END", nativeQuery = true)
    List<SettlementProjection> calculateSettlementAmounts(
            @Param("status") String status);

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
        BigDecimal getGrossAmount();
    }

    /**
     * 특정 판매자의 정산 상세 내역(결제 및 예약 정보)을 조회합니다.
     */
    @Query(value = "SELECT " +
            "p.id AS paymentId, " +
            "r.id AS reservationId, " +
            "r.target_type AS targetType, " +
            "CASE " +
            "  WHEN r.target_type = 'ROOM' THEN a.name " +
            "  WHEN r.target_type = 'CAR' THEN c.model_name " +
            "  ELSE '알 수 없음' " +
            "END AS productName, " +
            "p.total_amount AS amount, " +
            "p.created_at AS paymentDate " +
            "FROM payments p " +
            "JOIN reservations r ON p.reservation_id = r.id " +
            "LEFT JOIN rooms rm ON r.target_type = 'ROOM' AND r.target_id = rm.id " +
            "LEFT JOIN accommodations a ON rm.accommodation_id = a.id " +
            "LEFT JOIN rental_cars c ON r.target_type = 'CAR' AND r.target_id = c.id " +
            "WHERE p.settlement_id = :settlementId", nativeQuery = true)
    List<SettlementDetailProjection> findSettlementDetails(
            @Param("settlementId") Long settlementId);

    @Query(value = "SELECT p.* FROM payments p " +
            "JOIN reservations r ON p.reservation_id = r.id " +
            "LEFT JOIN rooms rm ON r.target_type = 'ROOM' AND r.target_id = rm.id " +
            "LEFT JOIN accommodations a ON rm.accommodation_id = a.id " +
            "LEFT JOIN rental_cars c ON r.target_type = 'CAR' AND r.target_id = c.id " +
            "WHERE p.status = :status " +
            "AND p.settlement_id IS NULL " +
            "AND (" +
            "  (r.target_type = 'ROOM' AND a.seller_id = :sellerId) OR " +
            "  (r.target_type = 'CAR' AND c.seller_id = :sellerId)" +
            ")", nativeQuery = true)
    List<Payment> findUnsettledPayments(
            @Param("sellerId") Long sellerId,
            @Param("status") String status);

    /**
     * 정산 상세 조회를 위한 Spring Data JPA 프로젝션 인터페이스입니다.
     */
    interface SettlementDetailProjection {
        Long getPaymentId();
        Long getReservationId();
        String getTargetType();
        String getProductName();
        BigDecimal getAmount();
        LocalDateTime getPaymentDate();
    }

    /**
     * 특정 결제 상태인 전체 결제 건의 총거래액(GMV)과 수수료 순이익(매출의 3%)을 합산하여 조회합니다.
     *
     * @param status 집계할 결제 상태 (일반적으로 PAID)
     * @return 플랫폼 전체 총거래액 및 수수료 금액 프로젝션
     */
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) AS totalGmv, " +
            "COALESCE(SUM(p.totalAmount) * 0.03, 0) AS totalCommission " +
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
        BigDecimal getTotalGmv();
        BigDecimal getTotalCommission();
    }

    /**
     * 서비스별 매출 집계를 위한 프로젝션 인터페이스입니다.
     */
    interface ServiceRevenueProjection {
        String getServiceType();
        BigDecimal getServiceGmv();
    }
}

