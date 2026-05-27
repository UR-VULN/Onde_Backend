package com.onde.api.application.payment;

import com.onde.api.application.payment.dto.request.*;
import com.onde.api.application.payment.dto.response.*;
import com.onde.api.application.mileage.MileageService;
import com.onde.api.application.membergrade.MemberGradeService;
import com.onde.core.entity.payment.MileageLogType;
import com.onde.core.entity.payment.Payment;
import com.onde.core.entity.payment.PaymentStatus;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.ReservationRepository;
import com.onde.api.infrastructure.portone.PortOneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 서비스 클래스입니다.
 * 결제 사전 검증 및 PENDING 데이터 저장, 사후 검증을 통한 PAID 확정 및 마일리지 처리,
 * 결제 취소에 따른 환불 및 마일리지 롤백(원상복구) 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MileageService mileageService;
    private final MemberGradeService memberGradeService;
    private final ReservationRepository reservationRepository;
    private final PortOneService portOneService;

    /**
     * PG 결제창 진입 전 사전 등록 및 검증을 수행합니다.
     * 사용자가 보유한 마일리지 범위 내에서 차감액을 지정했는지 검증하고,
     * 고유 주문 번호(merchantUid)를 생성한 후 PENDING(대기) 상태의 Payment 레코드를 생성합니다.
     *
     * @param userId 결제를 진행하는 회원의 식별자
     * @param req    총 결제 대상 금액과 마일리지 사용액이 포함된 요청 DTO
     * @return 생성된 주문번호(merchantUid)와 실제 PG사에 청구할 금액이 담긴 응답 DTO
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(Long userId, PaymentPrepareRequest req) {
        // 1. 현재 사용자가 보유한 사용 가능한 실시간 마일리지 잔액 조회
        int currentMileage = mileageService.getCurrentMileage(userId);
        if (req.getUsedMileage() != null && req.getUsedMileage() > currentMileage) {
            throw new IllegalArgumentException("사용 가능한 마일리지를 초과했습니다.");
        }

        // 2. 마일리지 사용액과 실제 PG사 청구액 계산
        Long usedMileage = req.getUsedMileage() != null ? req.getUsedMileage() : 0L;
        Long pgAmount = req.getTotalAmount() - usedMileage;

        // 3. 고유한 주문번호 생성 (포맷: ORDER-연도-난수8글자)
        String merchantUid = "ORDER-" + LocalDateTime.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8);

        // 4. 사후 검증 단계에서 비교 및 변경하기 위해 PENDING 상태로 결제 건을 미리 DB에 저장
        Payment pendingPayment = Payment.builder()
                .userId(userId)
                .reservationId(req.getReservationId())
                .reservationType(req.getReservationType())
                .totalAmount(req.getTotalAmount())
                .pgAmount(pgAmount)
                .usedMileage(usedMileage)
                .accumulatedMileage(0L) // 아직 결제 성공 전이므로 적립 마일리지는 0
                .status(PaymentStatus.PENDING)
                .merchantUid(merchantUid)
                .build();

        paymentRepository.save(pendingPayment);

        return PaymentPrepareResponse.builder()
                .merchantUid(merchantUid)
                .pgAmount(pgAmount)
                .usedMileage(usedMileage)
                .reservationId(req.getReservationId())
                .build();
    }

    /**
     * 포트원 등 PG사를 통한 결제 완료 후 백엔드 서버 측 사후 검증 및 승인 처리를 수행합니다.
     * 위변조 여부(실제 결제 금액 일치 여부)를 검증하고, 회원 등급에 맞는 적립율을 계산하여
     * 사용 마일리지 차감 및 신규 마일리지 적립을 하나의 트랜잭션 내에서 처리합니다.
     *
     * @param userId 결제한 회원의 식별자
     * @param req    포트원에서 반환된 거래 고유 ID(impUid) 및 주문번호(merchantUid)와 금액 정보를 담은 DTO
     * @return 결제 완료 상세 정보가 포함된 응답 DTO
     */
    @Transactional
    public PaymentValidateResponse validatePayment(Long userId, PaymentValidateRequest req) {
        // 1. 사전에 등록했던 PENDING 상태의 결제 정보 조회
        Payment payment = paymentRepository.findByMerchantUid(req.getMerchantUid())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다."));

        // 2. 금액 위변조 여부 검증 (사전에 약속된 PG 청구금액과 실제 결제 금액 비교)
        if (!payment.getPgAmount().equals(req.getPgAmount())) {
            throw new IllegalArgumentException("결제 요청 금액이 일치하지 않습니다.");
        }

        // --- PG사 실제 결제 내역 조회 및 실 금액 검증 ---
        PortOneService.PaymentAnnotation pgInfo = portOneService.getPaymentInfo(req.getImpUid(), payment.getPgAmount());
        if (!payment.getPgAmount().equals(pgInfo.getAmount())) {
            throw new IllegalArgumentException("실제 PG 결제 금액과 요청된 결제 금액이 일치하지 않습니다. (위변조 위험)");
        }
        if (!"paid".equals(pgInfo.getStatus())) {
            throw new IllegalArgumentException("PG사 결제 상태가 완료(paid) 상태가 아닙니다.");
        }

        // 3. 중복 처리 방지 검증
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("이미 결제 처리된 주문입니다.");
        }

        // 4. 회원 등급 기반 마일리지 적립률 계산 (누적 결제액에 따른 등급 및 적립률)
        Map<String, Object> gradeInfo = memberGradeService.getMemberGradeInfo(userId);
        double rate = (double) gradeInfo.get("rate");
        Long accumulatedMileage = (long) (req.getPgAmount() * rate);

        // 5. 결제 건 상태를 PAID로 승인 및 적립 마일리지 세팅
        payment.setImpUid(req.getImpUid());
        payment.setAccumulatedMileage(accumulatedMileage);
        payment.setStatus(PaymentStatus.PAID);

        // 6. 복합 결제에 사용된 마일리지를 차감 처리 (마일리지 로그 음수(-) 기록)
        if (payment.getUsedMileage() > 0) {
            mileageService.addLog(userId, (int) -payment.getUsedMileage(), MileageLogType.USE,
                    "결제 시 마일리지 사용 (" + req.getMerchantUid() + ")");
        }
        // 7. 실 결제액(PG 결제 금액)에 등급 적립률을 적용한 마일리지 적립 처리 (마일리지 로그 양수(+) 기록)
        if (accumulatedMileage > 0) {
            mileageService.addLog(userId, accumulatedMileage.intValue(), MileageLogType.EARN,
                    "결제 적립 (" + req.getMerchantUid() + ")");
        }

        paymentRepository.save(payment);

        return PaymentValidateResponse.builder()
                .paymentId(payment.getId())
                .impUid(payment.getImpUid())
                .totalAmount(payment.getTotalAmount())
                .pgAmount(payment.getPgAmount())
                .usedMileage(payment.getUsedMileage())
                .accumulatedMileage(payment.getAccumulatedMileage())
                .status(payment.getStatus())
                .build();
    }

    /**
     * 완료된 결제 건에 대해 취소 및 환불을 진행합니다.
     * 결제 상태를 CANCELLED로 변경하고, 결제 당시 차감했던 마일리지는 다시 복구(RESTORE)하고,
     * 결제 완료로 적립되었던 마일리지는 다시 회수(REVOKE)하는 롤백 트랜잭션을 수행합니다.
     *
     * @param userId    결제 취소를 요청한 사용자 식별자
     * @param paymentId 취소하고자 하는 결제 식별자
     * @param req       취소 사유 등이 명시된 요청 DTO
     * @return 환불 및 마일리지 복구/회수 결과 응답 DTO
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(Long userId, Long paymentId, PaymentCancelRequest req) {
        // 1. 대상 결제 건 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 건입니다."));

        // 2. 연관된 예약 조회를 통한 권한 검증 (구매자 또는 판매자인지 확인)
        Reservation reservation = reservationRepository.findById(payment.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("연관된 예약 정보가 존재하지 않습니다."));

        boolean isBuyer = payment.getUserId().equals(userId);
        boolean isSeller = reservation.getSellerId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new IllegalArgumentException("해당 예약을 취소할 권한이 없습니다.");
        }

        // 3. 이미 취소되었는지 상태 검증
        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalArgumentException("이미 취소 혹은 환불된 결제입니다.");
        }

        // --- 외부 PG사 결제 취소(환불) API 호출 ---
        if (payment.getImpUid() != null) {
            portOneService.cancelPayment(payment.getImpUid(), payment.getPgAmount(), req.getReason());
        }

        // 4. 결제 상태를 CANCELLED로 변경
        payment.setStatus(PaymentStatus.CANCELLED);

        // 5. 결제 시 사용했던 마일리지 돌려주기 (마일리지 로그 양수(+) 기록) - 실구매자(payment.getUserId()) 기준
        if (payment.getUsedMileage() > 0) {
            mileageService.addLog(payment.getUserId(), payment.getUsedMileage().intValue(), MileageLogType.RESTORE,
                    "결제 취소 복구 (" + req.getReason() + ")");
        }
        // 6. 결제 성공으로 적립되었던 마일리지 회수 (마일리지 로그 음수(-) 기록) - 실구매자(payment.getUserId()) 기준
        if (payment.getAccumulatedMileage() > 0) {
            mileageService.addLog(payment.getUserId(), (int) -payment.getAccumulatedMileage(), MileageLogType.REVOKE,
                    "결제 취소 적립취소 (" + req.getReason() + ")");
        }

        return PaymentCancelResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .refundedAmount(payment.getPgAmount())
                .restoredMileage(payment.getUsedMileage())
                .cancelledAt(LocalDateTime.now())
                .build();
    }
}
