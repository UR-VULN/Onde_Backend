package com.onde.api.application.dashboard;

import com.onde.api.application.dashboard.dto.DashboardResponse;
import com.onde.api.application.dashboard.dto.SellerDashboardRevealResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.flight.FlightRoute;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.payment.Payment;
import com.onde.core.entity.settlement.Settlement;
import com.onde.core.entity.settlement.SettlementStatus;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.repository.*;
import com.onde.core.security.PersonalDataMasker;
import com.onde.core.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final SellerAccountRepository sellerAccountRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final CarRepository carRepository;
    private final FlightRouteRepository flightRouteRepository;
    private final ReservationRepository reservationRepository;
    private final FlightBookingRepository flightBookingRepository;
    private final MemberRepository memberRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final AesUtil aesUtil;

    public DashboardResponse getDashboardInfo(Member member) {
        Long sellerId = member.getId();

        SellerAccount sellerAccount = sellerAccountRepository.findByMemberId(sellerId)
                .orElse(null);

        // 1. 등록 상품 수 카운트
        List<Accommodation> accommodations = accommodationRepository.findBySellerId(sellerId);
        long accommodationCount = accommodations.size();
        
        List<Car> cars = carRepository.findBySellerId(sellerId);
        long carCount = cars.size();
        
        List<FlightRoute> routes = flightRouteRepository.findBySellerId(sellerId);
        long flightRouteCount = routes.size();

        // 2. 통합 예약 리스트 추출 및 항목별 매출 집계
        List<DashboardResponse.RecentReservationDto> allReservations = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        long stayRevenue = 0;
        long carRevenue = 0;
        long flightRevenue = 0;

        List<Settlement> completedSettlements = settlementRepository.findBySellerIdAndStatus(sellerId, SettlementStatus.COMPLETED);
        for (Settlement s : completedSettlements) {
            List<Payment> payments = paymentRepository.findBySettlementId(s.getId());
            for (Payment p : payments) {
                if (p.getReservationType() != null) {
                    long pNetAmount = p.getTotalAmount().longValue() - (long) Math.floor(p.getTotalAmount().doubleValue() * 0.03);
                    if (p.getReservationType().equals("ROOM")) {
                        stayRevenue += pNetAmount;
                    } else if (p.getReservationType().equals("CAR")) {
                        carRevenue += pNetAmount;
                    } else if (p.getReservationType().equals("FLIGHT")) {
                        flightRevenue += pNetAmount;
                    }
                }
            }
        }

        // (1) 숙소 예약 추출 및 매출 계산
        List<Long> roomIds = accommodations.stream()
                .flatMap(acc -> roomRepository.findByAccommodationId(acc.getId()).stream())
                .map(Room::getId)
                .collect(Collectors.toList());
        
        if (!roomIds.isEmpty()) {
            List<Reservation> hotelReservations = reservationRepository.findByTargetTypeAndTargetIdInOrderByCreatedAtDesc(
                    ReservationTarget.ROOM, roomIds);
            
            for (Reservation res : hotelReservations) {
                // RESERVED, CANCELLED 상태 제외하고 CONFIRMED, COMPLETED 상태만 표시
                if (res.getStatus() != ReservationStatus.CONFIRMED && res.getStatus() != ReservationStatus.COMPLETED) {
                    continue;
                }

                String roomName = roomRepository.findById(res.getTargetId())
                        .map(Room::getName)
                        .orElse("객실 정보 없음");
                
                String customerName = memberRepository.findById(res.getUserId())
                        .map(Member::getName)
                        .map(PersonalDataMasker::maskName)
                        .orElse("회원");

                allReservations.add(DashboardResponse.RecentReservationDto.builder()
                        .id(res.getId())
                        .customerName(customerName)
                        .targetType("STAY")
                        .productName(roomName)
                        .schedule(res.getCheckIn().format(formatter) + " ~ " + res.getCheckOut().format(formatter))
                        .price(res.getTotalPrice().longValue())
                        .status(res.getStatus().name())
                        .build());
            }
        }

        // (2) 렌터카 예약 추출 및 매출 계산
        List<Long> carIds = cars.stream().map(Car::getId).collect(Collectors.toList());
        if (!carIds.isEmpty()) {
            List<Reservation> carReservations = reservationRepository.findByTargetTypeAndTargetIdInOrderByCreatedAtDesc(
                    ReservationTarget.CAR, carIds);
            
            for (Reservation res : carReservations) {
                // RESERVED, CANCELLED 상태 제외하고 CONFIRMED, COMPLETED 상태만 표시
                if (res.getStatus() != ReservationStatus.CONFIRMED && res.getStatus() != ReservationStatus.COMPLETED) {
                    continue;
                }

                String modelName = carRepository.findById(res.getTargetId())
                        .map(Car::getModelName)
                        .orElse("차량 정보 없음");
                
                String customerName = memberRepository.findById(res.getUserId())
                        .map(Member::getName)
                        .map(PersonalDataMasker::maskName)
                        .orElse("회원");

                allReservations.add(DashboardResponse.RecentReservationDto.builder()
                        .id(res.getId())
                        .customerName(customerName)
                        .targetType("CAR")
                        .productName(modelName)
                        .schedule(res.getCheckIn().format(formatter) + " ~ " + res.getCheckOut().format(formatter))
                        .price(res.getTotalPrice().longValue())
                        .status(res.getStatus().name())
                        .build());
            }
        }

        // (3) 항공 노선 예약 추출 및 매출 계산
        List<FlightBooking> flightBookings = flightBookingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        for (FlightBooking fb : flightBookings) {
            // CONFIRMED인 결제 완료 상태일 때만 표시
            if (fb.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }

            String routeName = fb.getFlightSchedule().getRoute().getDepartureAirport() + " -> " + 
                               fb.getFlightSchedule().getRoute().getArrivalAirport();
            
            String customerName = memberRepository.findById(fb.getUserId())
                    .map(Member::getName)
                    .map(PersonalDataMasker::maskName)
                    .orElse("회원");

            allReservations.add(DashboardResponse.RecentReservationDto.builder()
                    .id(fb.getId())
                    .customerName(customerName)
                    .targetType("FLIGHT")
                    .productName(routeName)
                    .schedule(fb.getFlightSchedule().getDepartureTime().format(formatter))
                    .price(fb.getTotalPrice().longValue())
                    .status(fb.getStatus().name())
                    .build());
        }

        // 3. 최신 예약 등록 순 정렬 및 상위 5건 절삭 (ID 기준 내림차순)
        List<DashboardResponse.RecentReservationDto> recentList = allReservations.stream()
                .sorted(Comparator.comparing(DashboardResponse.RecentReservationDto::getId).reversed())
                .limit(5)
                .collect(Collectors.toList());

        long totalRevenue = stayRevenue + carRevenue + flightRevenue;

        String plainAccountNumber = resolvePlainAccountNumber(sellerAccount);

        return DashboardResponse.of(
                member, 
                sellerAccount,
                plainAccountNumber,
                accommodationCount,
                carCount,
                flightRouteCount,
                stayRevenue,
                carRevenue,
                flightRevenue,
                totalRevenue,
                recentList
        );

    }

    private String resolvePlainAccountNumber(SellerAccount sellerAccount) {
        if (sellerAccount == null || sellerAccount.getAccountNumber() == null
                || sellerAccount.getAccountNumber().isBlank()) {
            return null;
        }
        String stored = sellerAccount.getAccountNumber().trim();
        try {
            String decrypted = aesUtil.decrypt(stored);
            if (decrypted != null && !decrypted.isBlank()) {
                return decrypted;
            }
        } catch (RuntimeException ignored) {
            // DB에 평문으로 저장된 레거시 데이터
        }
        return stored;
    }

    public SellerDashboardRevealResponse getDashboardReveal(Member member) {
        SellerAccount sellerAccount = sellerAccountRepository.findByMemberId(member.getId()).orElse(null);
        return SellerDashboardRevealResponse.builder()
                .email(member.getEmail())
                .bankName(sellerAccount != null ? sellerAccount.getBankName() : null)
                .accountNumber(resolvePlainAccountNumber(sellerAccount))
                .build();
    }
}
