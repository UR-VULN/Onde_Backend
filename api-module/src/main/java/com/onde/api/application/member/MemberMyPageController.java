package com.onde.api.application.member;

import com.onde.api.application.member.dto.MemberProfileRevealResponse;
import com.onde.api.application.member.dto.MemberProfileResponse;
import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import com.onde.api.application.member.dto.ProfileUpdateRequestDto;
import com.onde.api.application.member.dto.SellerProfileResponse;
import com.onde.api.application.member.dto.SellerProfileUpdateRequest;
import com.onde.api.application.member.dto.SensitiveRevealPasswordRequest;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping
@RequiredArgsConstructor
public class MemberMyPageController {

    private final MemberMyPageService memberMyPageService;
    private final SensitiveRevealAuthService sensitiveRevealAuthService;

    @GetMapping({"/api/v1/members/me/profile", "/v1/user/profile"})
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @LoginMember(required = true) Long userId) {
        MemberProfileResponse response = memberMyPageService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필 정보 조회 성공"));
    }

    @PostMapping({"/api/v1/members/me/profile/reveal", "/v1/user/profile/reveal"})
    public ResponseEntity<ApiResponse<MemberProfileRevealResponse>> revealMyProfile(
            @LoginMember(required = true) Long userId,
            @Valid @RequestBody SensitiveRevealPasswordRequest request) {
        sensitiveRevealAuthService.requirePasswordVerifiedMember(userId, request.getPassword());
        MemberProfileRevealResponse response = memberMyPageService.getProfileReveal(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필 원문 조회 성공"));
    }

    @PatchMapping({"/api/v1/members/me/profile", "/v1/user/profile"})
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @LoginMember(required = true) Long userId,
            @Valid @RequestBody ProfileUpdateRequestDto requestDto) {
        memberMyPageService.updateProfile(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "프로필 정보가 수정되었습니다."));
    }

    @GetMapping("/api/v1/seller/profile")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getSellerProfile(
            @LoginMember(required = true) Long userId) {
        SellerProfileResponse response = memberMyPageService.getSellerProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "판매자 프로필 정보 조회 성공"));
    }

    @PatchMapping("/api/v1/seller/profile")
    public ResponseEntity<ApiResponse<Void>> updateSellerProfile(
            @LoginMember(required = true) Long userId,
            @Valid @RequestBody SellerProfileUpdateRequest requestDto) {
        memberMyPageService.updateSellerProfile(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "판매자 프로필 정보가 수정되었습니다."));
    }

    @GetMapping("/api/v1/members/me/reservations/flights")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageFlightBookingResponse>>> getMyFlightBookings(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) @Size(max = 20) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageFlightBookingResponse> response = memberMyPageService.getMyFlightBookings(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "항공 예약 목록 조회 성공"));
    }

    @GetMapping("/api/v1/members/me/insurances")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageInsurancePolicyResponse>>> getMyInsurancePolicies(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) @Size(max = 20) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageInsurancePolicyResponse> response = memberMyPageService.getMyInsurancePolicies(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "보험 가입 목록 조회 성공"));
    }

    @GetMapping("/api/v1/members/me/reservations/rooms")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageRoomReservationResponse>>> getMyRoomReservations(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) @Size(max = 20) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageRoomReservationResponse> response = memberMyPageService.getMyRoomReservations(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "숙소 예약 목록 조회 성공"));
    }

    @GetMapping("/api/v1/members/me/reservations/cars")
    public ResponseEntity<ApiResponse<MyPageListResponse<MyPageCarReservationResponse>>> getMyCarReservations(
            @LoginMember(required = true) Long userId,
            @RequestParam(name = "status", required = false) @Size(max = 20) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Pageable pageable = PageRequest.of(page, size);
        MyPageListResponse<MyPageCarReservationResponse> response = memberMyPageService.getMyCarReservations(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "렌터카 예약 목록 조회 성공"));
    }

    @DeleteMapping("/api/v1/members/me/reservations/flights/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelMyFlightBooking(
            @LoginMember(required = true) Long userId,
            @PathVariable @Min(1) Long bookingId) {
        memberMyPageService.cancelFlightBooking(userId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "항공 예약이 취소되었습니다."));
    }

    @DeleteMapping("/api/v1/members/me/insurances/{policyId}")
    public ResponseEntity<ApiResponse<Void>> cancelMyInsurancePolicy(
            @LoginMember(required = true) Long userId,
            @PathVariable @Min(1) Long policyId) {
        memberMyPageService.cancelInsurancePolicy(userId, policyId);
        return ResponseEntity.ok(ApiResponse.success(null, "보험 가입이 취소되었습니다."));
    }
}
