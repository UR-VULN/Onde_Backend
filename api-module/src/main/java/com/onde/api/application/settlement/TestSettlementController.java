package com.onde.api.application.settlement;

import com.onde.api.application.settlement.SettlementService;
import com.onde.core.entity.payment.Payment;
import com.onde.core.entity.payment.PaymentStatus;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.PaymentRepository;
import com.onde.core.repository.SellerAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 정산 및 마일리지 로직 검증을 위해 더미 데이터를 적재하고 강제 배치를 수행하는 테스트 전용 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/test/settlement")
public class TestSettlementController {

    private final SettlementService settlementService;
    private final PaymentRepository paymentRepository;
    private final JpaRepository<Reservation, Long> reservationRepository;
    private final SellerAccountRepository sellerAccountRepository;
    private final com.onde.core.repository.MileageLogRepository mileageLogRepository;
    private final com.onde.api.infrastructure.portone.PortOneService portOneService;
    private final ApplicationContext context;

    @Autowired
    @SuppressWarnings("unchecked")
    public TestSettlementController(SettlementService settlementService, PaymentRepository paymentRepository, 
                                    ApplicationContext context, 
                                    com.onde.core.repository.MileageLogRepository mileageLogRepository,
                                    SellerAccountRepository sellerAccountRepository,
                                    com.onde.api.infrastructure.portone.PortOneService portOneService) {
        this.settlementService = settlementService;
        this.paymentRepository = paymentRepository;
        this.context = context;
        // ReservationRepository를 멀티모듈 빈 조회 방식으로 동적 주입합니다.
        this.reservationRepository = context.getBean("reservationRepository", JpaRepository.class);
        this.mileageLogRepository = mileageLogRepository;
        this.sellerAccountRepository = sellerAccountRepository;
        this.portOneService = portOneService;
    }

    /**
     * 전월 기준의 더미 예약, 결제 완료(PAID) 내역 및 테스트용 마일리지 변동 이력을 데이터베이스에 생성합니다.
     * 이 더미 데이터를 생성해 두면 전월에 대한 월간 정산 스케줄러 기능 및 회원 등급 실시간 조회 기능의 테스트가 가능합니다.
     * 1번 사용자의 누적 결제액을 약 115만원으로 설정하여 실시간 등급 계산 시 GOLD(적립률 1.0%)가 산출되도록 유도합니다.
     *
     * @return 더미 생성 완료 안내 메시지
     */
    @PostMapping("/dummy")
    public ResponseEntity<String> createDummyData(
            @RequestParam(name = "userId", required = false, defaultValue = "1") Long userId,
            @RequestParam(name = "sellerId", required = false, defaultValue = "1") Long sellerId) {
        
        // JpaRepository를 사용해 accommodation 및 room 저장소 동적 주입
        JpaRepository accommodationRepository = context.getBean("accommodationRepository", JpaRepository.class);
        JpaRepository roomRepository = context.getBean("roomRepository", JpaRepository.class);

        // 1. 1번 숙소 및 1번 방 생성/업데이트 (소유자를 파라미터로 받은 sellerId로 강제 지정)
        com.onde.core.entity.accommodation.Accommodation accommodation = new com.onde.core.entity.accommodation.Accommodation();
        accommodation.setId(1L);
        accommodation.setSellerId(sellerId);
        accommodation.setName("더미 테스트 숙소");
        accommodation.setApprovalStatus(com.onde.core.entity.accommodation.ApprovalStatus.APPROVED);
        accommodationRepository.save(accommodation);

        com.onde.core.entity.accommodation.Room room = new com.onde.core.entity.accommodation.Room();
        room.setId(1L);
        room.setAccommodation(accommodation);
        room.setName("더미 테스트 방");
        roomRepository.save(room);

        // 2. 결제 발생 일시를 전일(어제)로 설정하여 일간 정산 조건 충족
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayMidday = yesterday.atTime(12, 0);
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);

        // 2. 판매자 및 사용자 ID에 대한 더미 예약 데이터 생성 및 저장
        Reservation r1 = Reservation.builder()
                .userId(userId)
                .targetType(com.onde.core.entity.reservation.ReservationTarget.ROOM)
                .targetId(1L)
                .checkIn(LocalDateTime.now())
                .checkOut(LocalDateTime.now().plusDays(1))
                .status(com.onde.core.entity.reservation.ReservationStatus.COMPLETED)
                .totalPrice(java.math.BigDecimal.valueOf(1200000))
                .build();
        Reservation r2 = Reservation.builder()
                .userId(sellerId)
                .targetType(com.onde.core.entity.reservation.ReservationTarget.ROOM)
                .targetId(2L)
                .checkIn(LocalDateTime.now())
                .checkOut(LocalDateTime.now().plusDays(1))
                .status(com.onde.core.entity.reservation.ReservationStatus.COMPLETED)
                .totalPrice(java.math.BigDecimal.valueOf(200000))
                .build();
        
        if (reservationRepository != null) {
            reservationRepository.saveAll(Arrays.asList(r1, r2));
        }

        // 3. 결제 완료 상태(PAID)의 결제 내역 더미 데이터 리스트 생성
        List<Payment> dummies = Arrays.asList(
                // 판매자 대상의 첫 번째 결제 건 (총 120만원, 마일리지 5만원 사용, PG 실 결제 115만원, 적립 1.15만 마일리지)
                Payment.builder()
                        .userId(userId)
                        .reservationId(r1.getId() != null ? r1.getId() : 1L)
                        .totalAmount(1200000L).pgAmount(1150000L).usedMileage(50000L).accumulatedMileage(11500L)
                        .impUid("imp_0001_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .merchantUid("merchant_0001_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .status(PaymentStatus.PAID)
                        .createdAt(yesterdayMidday)
                        .build(),
                // 판매자 대상의 두 번째 결제 건 (총 5만원)
                Payment.builder()
                        .userId(101L)
                        .reservationId(r1.getId() != null ? r1.getId() : 1L)
                        .totalAmount(50000L).pgAmount(50000L).usedMileage(0L).accumulatedMileage(500L)
                        .impUid("imp_0002_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .merchantUid("merchant_0002_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .status(PaymentStatus.PAID)
                        .createdAt(yesterdayMidday)
                        .build(),
                // 판매자 2번 대상의 세 번째 결제 건 (총 20만원, 마일리지 2만원 사용, PG 실 결제 18만원)
                Payment.builder()
                        .userId(102L)
                        .reservationId(r2.getId() != null ? r2.getId() : 2L)
                        .totalAmount(200000L).pgAmount(180000L).usedMileage(20000L).accumulatedMileage(1800L)
                        .impUid("imp_0003_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .merchantUid("merchant_0003_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                        .status(PaymentStatus.PAID)
                        .createdAt(yesterdayMidday)
                        .build()
        );

        paymentRepository.saveAll(dummies);

        // 4. 판매자 1번에 대한 정산 계좌 정보 생성 및 저장 (9번 API인 정산 신청 단계에서 필수 검증됨)
        if (!sellerAccountRepository.findByMemberId(sellerId).isPresent()) {
            sellerAccountRepository.save(com.onde.core.entity.settlement.SellerAccount.builder()
                    .member(Member.builder().id(sellerId).build())
                    .bankName("신한은행")
                    .accountNumber("110-123-456789")
                    .accountHolder("홍길동")
                    .businessNumber("123-45-67890")
                    .representativeName("홍길동")
                    .openedAt("20200101")
                    .build());
        }

        // 5. 테스트 대상인 사용자(userId)의 마일리지 변동 이력(MileageLog) 적재
        List<com.onde.core.entity.payment.MileageLog> mileageDummies = Arrays.asList(
                // 5만원 웰컴 적립
                com.onde.core.entity.payment.MileageLog.builder()
                        .userId(userId)
                        .amount(50000)
                        .logType(com.onde.core.entity.payment.MileageLogType.EARN)
                        .description("회원가입 기념 웰컴 마일리지 적립")
                        .createdAt(lastMonth.minusDays(5))
                        .build(),
                // 1만원 사용 차감
                com.onde.core.entity.payment.MileageLog.builder()
                        .userId(userId)
                        .amount(-10000)
                        .logType(com.onde.core.entity.payment.MileageLogType.USE)
                        .description("숙소 예약 시 마일리지 사용")
                        .createdAt(lastMonth.minusDays(2))
                        .build(),
                // 위 결제 1번 건에 대한 적립분 (1.15만)
                com.onde.core.entity.payment.MileageLog.builder()
                        .userId(userId)
                        .amount(11500)
                        .logType(com.onde.core.entity.payment.MileageLogType.EARN)
                        .description("숙소 이용 완료 결제 적립 (1.0%)")
                        .createdAt(lastMonth.plusDays(2))
                        .build()
        );
        mileageLogRepository.saveAll(mileageDummies);

        return ResponseEntity.ok("더미 데이터 생성 완료 (유저 " + userId + "번 누적 결제액 115만원으로 설정 -> GOLD 등급 및 마일리지 로그 추가, 판매자 " + sellerId + "번 정산 계좌 등록 완료)");
    }

    /**
     * 일간 정산 스케줄러(배치) 로직을 수동으로 즉시 실행시키는 테스트용 엔드포인트입니다.
     * 어제 전체 기간 동안의 거래액을 정산하여 Settlement 테이블에 레코드를 저장합니다.
     *
     * @return 강제 실행 완료 결과 메시지
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeSettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        settlementService.executeDailySettlement(yesterday);
        settlementService.executeDailySettlement(today); // JPA Auditing에 의해 오늘 날짜로 저장된 더미 결제 정보까지 정산 처리
        return ResponseEntity.ok("일간 정산 배치 강제 실행 완료. 대상 일자: " + yesterday + " 및 " + today + ". DB(Settlement)를 확인해보세요.");
    }

    /**
     * 포트원 실제 외부 API 연동을 테스트하기 위한 검증용 엔드포인트입니다.
     * 기입된 API Key와 Secret을 사용하여 포트원 서버로부터 Access Token 발급 시도 여부를 응답합니다.
     */
    @GetMapping("/test-connection")
    public ResponseEntity<String> testPortOneConnection() {
        try {
            String token = portOneService.getAccessToken();
            return ResponseEntity.ok("포트원 외부 API 연동 성공! 발급받은 Access Token: " + token);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("포트원 외부 API 연동 실패! 에러 메시지: " + e.getMessage());
        }
    }
}

