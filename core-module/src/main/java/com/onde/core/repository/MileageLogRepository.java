package com.onde.core.repository;

import com.onde.core.entity.payment.MileageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * MileageLog 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface MileageLogRepository extends JpaRepository<MileageLog, Long> {

    /**
     * 특정 사용자의 마일리지 변동 이력을 페이징하여 조회합니다.
     *
     * @param userId   조회할 사용자의 식별자
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬 등)
     * @return 페이징된 마일리지 로그 목록
     */
    Page<MileageLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 현재 사용 가능한 총 마일리지 잔액을 계산합니다.
     * 원장(Ledger) 구조를 기반으로 하기 때문에 사용자의 모든 마일리지 로그(amount)의 총합을 구합니다.
     * 마일리지 로그가 존재하지 않을 경우 COALESCE 함수를 통해 기본값 0을 반환합니다.
     *
     * @param userId 잔액을 조회할 사용자의 식별자
     * @return 사용자의 현재 총 마일리지 잔액
     */
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MileageLog m WHERE m.userId = :userId")
    Integer calculateTotalMileageByUserId(@Param("userId") Long userId);
}

