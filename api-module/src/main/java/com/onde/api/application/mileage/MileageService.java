package com.onde.api.application.mileage;

import com.onde.core.entity.payment.MileageLog;
import com.onde.core.entity.payment.MileageLogType;
import com.onde.core.repository.MileageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 마일리지 잔액 계산, 이력 조회 및 마일리지 변동 기록 적재를 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class MileageService {

    private final MileageLogRepository mileageLogRepository;

    /**
     * 특정 사용자의 현재 사용 가능한 실시간 총 마일리지 잔액을 구합니다.
     * 마일리지 변동 내역 테이블(mileage_logs)의 amount 합계(SUM)로 집계됩니다.
     *
     * @param userId 회원 식별자
     * @return 사용자의 현재 마일리지 잔액
     */
    @Transactional(readOnly = true)
    public int getCurrentMileage(Long userId) {
        return mileageLogRepository.calculateTotalMileageByUserId(userId);
    }

    /**
     * 특정 사용자의 마일리지 변동 이력을 페이징하여 조회합니다.
     *
     * @param userId   회원 식별자
     * @param pageable 페이징 설정 정보
     * @return 페이징된 마일리지 로그 목록
     */
    @Transactional(readOnly = true)
    public Page<MileageLog> getMileageHistory(Long userId, Pageable pageable) {
        return mileageLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 마일리지 변동 이력(적립, 차감, 복구, 회수)을 발생시켜 기록을 저장합니다.
     * 이 메서드는 결제 프로세스(PaymentService) 내에서 단일 트랜잭션으로 호출되어 원장을 갱신하는 데 사용됩니다.
     *
     * @param userId      회원 식별자
     * @param amount      마일리지 변동액 (적립은 양수(+), 차감/사용은 음수(-))
     * @param logType     변동 유형 (EARN, USE, RESTORE, REVOKE)
     * @param description 로그에 남길 구체적인 상세 설명
     * @return 저장된 MileageLog 엔티티 객체
     */
    @Transactional
    public MileageLog addLog(Long userId, int amount, MileageLogType logType, String description) {
        MileageLog log = MileageLog.builder()
                .userId(userId)
                .amount(amount)
                .logType(logType)
                .description(description)
                .build();
        return mileageLogRepository.save(log);
    }
}

