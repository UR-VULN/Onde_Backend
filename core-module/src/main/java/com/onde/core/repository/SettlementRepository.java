package com.onde.core.repository;

import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Settlement 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * 특정 판매자의 전체 정산 내역을 페이징하여 조회합니다.
     * 판매자 어드민 페이지에서 본인의 정산 내역을 조회할 때 사용됩니다.
     *
     * @param sellerId 조회할 판매자 식별자
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬 등)
     * @return 페이징된 정산 내역 목록
     */
    Page<Settlement> findBySellerId(Long sellerId, Pageable pageable);
    
    java.util.Optional<Settlement> findBySellerIdAndSettlementDate(Long sellerId, java.time.LocalDate settlementDate);
    
    boolean existsBySellerIdAndStatusIn(Long sellerId, List<SettlementStatus> statuses);

    List<Settlement> findBySellerIdAndStatus(Long sellerId, SettlementStatus status);

    /**
     * 특정 정산 진행 상태를 가진 정산 내역을 페이징하여 조회합니다.
     * 플랫폼 관리자가 특정 상태(예: REQUESTED, APPROVED_1ST 등)의 정산 대상 건을 승인/지급 처리하기 위해 목록을 조회할 때 사용됩니다.
     *
     * @param status   조회할 정산 상태
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬 등)
     * @return 페이징된 정산 내역 목록
     */
    Page<Settlement> findByStatus(SettlementStatus status, Pageable pageable);

    /**
     * 특정 판매자의 일자별 정산 매출액을 집계하여 조회합니다. (일별 매출 추이용)
     *
     * @param sellerId 조회할 판매자 식별자
     * @return 일자별 매출 및 순 정산 지급액 리스트
     */
    @Query("SELECT CAST(s.settlementDate AS string) AS label, " +
            "SUM(s.grossAmount) AS grossAmount, " +
            "SUM(s.netAmount) AS netAmount " +
            "FROM Settlement s " +
            "WHERE s.sellerId = :sellerId " +
            "GROUP BY s.settlementDate " +
            "ORDER BY s.settlementDate ASC")
    List<RevenueProjection> findDailyRevenueBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 특정 판매자의 월별 정산 매출액을 집계하여 조회합니다. (월별 매출 추이용)
     *
     * @param sellerId 조회할 판매자 식별자
     * @return 월별 매출 및 순 정산 지급액 리스트
     */
    @Query("SELECT SUBSTRING(CAST(s.settlementDate AS string), 1, 7) AS label, " +
            "SUM(s.grossAmount) AS grossAmount, " +
            "SUM(s.netAmount) AS netAmount " +
            "FROM Settlement s " +
            "WHERE s.sellerId = :sellerId " +
            "GROUP BY SUBSTRING(CAST(s.settlementDate AS string), 1, 7) " +
            "ORDER BY label ASC")
    List<RevenueProjection> findMonthlyRevenueBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 매출 통계 집계를 위한 프로젝션 인터페이스입니다.
     */
    interface RevenueProjection {
        String getLabel();
        BigDecimal getGrossAmount();
        BigDecimal getNetAmount();
    }
}


