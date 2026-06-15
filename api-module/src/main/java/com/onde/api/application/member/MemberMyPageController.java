package com.onde.api.application.member;

import com.onde.api.application.member.dto.MemberProfileResponse;
import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import com.onde.api.application.member.dto.ProfileUpdateRequestDto;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberMyPageController {

    private final MemberMyPageService memberMyPageService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @LoginMember(required = true) Long userId) {
        MemberProfileResponse response = memberMyPageService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필 정보 조회 성공"));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @LoginMember(required = true) Long userId,
            @Valid @RequestBody ProfileUpdateRequestDto requestDto) {
        memberMyPageService.updateProfile(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "프로필 정보가 수정되었습니다."));
    }

    @GetMapping("/reservations/flights")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageFlightBookingResponse>>> getMyFlightBookings(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageFlightBookingResponse> response = memberMyPageService.getMyFlightBookings(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "항공 예약 목록 조회 성공"));
    }


    @GetMapping("/insurances")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageInsurancePolicyResponse>>> getMyInsurancePolicies(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageInsurancePolicyResponse> response = memberMyPageService.getMyInsurancePolicies(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "보험 가입 목록 조회 성공"));
    }

    @GetMapping("/reservations/rooms")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageRoomReservationResponse>>> getMyRoomReservations(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageRoomReservationResponse> response = memberMyPageService.getMyRoomReservations(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "숙소 예약 목록 조회 성공"));
    }

    @GetMapping("/reservations/cars")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageCarReservationResponse>>> getMyCarReservations(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageCarReservationResponse> response = memberMyPageService.getMyCarReservations(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "렌터카 예약 목록 조회 성공"));
    }

    @DeleteMapping("/reservations/flights/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelMyFlightBooking(
            @LoginMember(required = true) Long userId,
            @PathVariable Long bookingId) {
        memberMyPageService.cancelFlightBooking(userId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "항공 예약이 취소되었습니다."));
    }

    @DeleteMapping("/insurances/{policyId}")
    public ResponseEntity<ApiResponse<Void>> cancelMyInsurancePolicy(
            @LoginMember(required = true) Long userId,
            @PathVariable Long policyId) {
        memberMyPageService.cancelInsurancePolicy(userId, policyId);
        return ResponseEntity.ok(ApiResponse.success(null, "보험 가입이 취소되었습니다."));
    }
}
