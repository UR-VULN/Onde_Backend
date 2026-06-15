package com.onde.api.application.dashboard;

import com.onde.api.application.dashboard.dto.DashboardResponse;
import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.Room;
import com.onde.core.entity.accommodation.Car;
import com.onde.core.entity.flight.FlightRoute;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.entity.reservation.ReservationTarget;
import com.onde.core.entity.settlement.SellerAccount;
import com.onde.core.repository.*;
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

        // (1) 숙소 예약 추출 및 매출 계산
        List<Long> roomIds = accommodations.stream()
                .flatMap(acc -> roomRepository.findByAccommodationId(acc.getId()).stream())
                .map(Room::getId)
                .collect(Collectors.toList());
        
        if (!roomIds.isEmpty()) {
            List<Reservation> hotelReservations = reservationRepository.findByTargetTypeAndTargetIdInOrderByCreatedAtDesc(
                    ReservationTarget.ROOM, roomIds);
            
            for (Reservation res : hotelReservations) {
                // CANCELLED 상태가 아닐 때만 매출에 합산
                if (res.getStatus() != com.onde.core.entity.reservation.ReservationStatus.CANCELLED) {
                    stayRevenue += res.getTotalPrice().longValue();
                }

                String roomName = roomRepository.findById(res.getTargetId())
                        .map(Room::getName)
                        .orElse("객실 정보 없음");
                
                String customerName = memberRepository.findById(res.getUserId())
                        .map(Member::getName)
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
                // CANCELLED 상태가 아닐 때만 매출에 합산
                if (res.getStatus() != com.onde.core.entity.reservation.ReservationStatus.CANCELLED) {
                    carRevenue += res.getTotalPrice().longValue();
                }

                String modelName = carRepository.findById(res.getTargetId())
                        .map(Car::getModelName)
                        .orElse("차량 정보 없음");
                
                String customerName = memberRepository.findById(res.getUserId())
                        .map(Member::getName)
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
            // CONFIRMED인 결제 완료 상태일 때만 매출에 합산
            if (fb.getStatus() == com.onde.core.entity.flight.BookingStatus.CONFIRMED) {
                flightRevenue += fb.getTotalPrice().longValue();
            }

            String routeName = fb.getFlightSchedule().getRoute().getDepartureAirport() + " -> " + 
                               fb.getFlightSchedule().getRoute().getArrivalAirport();
            
            String customerName = memberRepository.findById(fb.getUserId())
                    .map(Member::getName)
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

        return DashboardResponse.of(
                member, 
                sellerAccount,
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
}
