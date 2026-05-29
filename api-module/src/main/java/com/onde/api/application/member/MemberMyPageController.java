package com.onde.api.application.member;

import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
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
}
