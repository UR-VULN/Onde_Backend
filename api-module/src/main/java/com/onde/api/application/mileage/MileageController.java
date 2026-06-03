package com.onde.api.application.mileage;

import com.onde.api.application.membergrade.MemberGradeService;
import com.onde.api.application.mileage.MileageService;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.payment.MileageLog;
import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원(Member) 관점에서의 마일리지 조회 및 이력 관련 API를 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/v1/members/me/mileage")
@RequiredArgsConstructor
public class MileageController {

    private final MileageService mileageService;
    private final MemberGradeService memberGradeService;

    /**
     * 회원의 실시간 마일리지 잔액 정보와 누적 결제액 기준 회원 등급, 적립율, 다음 등급 기준액을 포함한 요약 정보를 반환합니다.
     *
     * @param userId 회원 식별자
     * @return 마일리지 잔액, 등급명, 적립율 등의 맵 데이터 응답
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMileageSummary(@LoginMember Long userId) {
        // 1. 사용자의 마일리지 잔액 조회
        int balance = mileageService.getCurrentMileage(userId);
        
        // 2. 사용자의 누적 결제 기반 회원 등급 정보 조회
        Map<String, Object> gradeInfo = memberGradeService.getMemberGradeInfo(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("memberId", userId);
        data.put("memberGrade", gradeInfo.get("grade"));
        data.put("mileageBalance", balance);
        data.put("accumulationRate", gradeInfo.get("rate"));
        data.put("nextGradeThreshold", gradeInfo.get("threshold"));
        
        return ResponseEntity.ok(ApiResponse.success(data, "마일리지 조회 성공"));
    }

    /**
     * 회원의 마일리지 적립/사용/복구/회수 이력을 페이징하여 조회합니다.
     *
     * @param userId 회원 식별자
     * @param page   조회할 페이지 번호 (0부터 시작)
     * @param size   한 페이지에 출력할 로그의 개수
     * @return 페이징된 마일리지 로그 목록 응답
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMileageHistory(
            @LoginMember Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
            
        Page<MileageLog> logs = mileageService.getMileageHistory(userId, PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("logs", logs.getContent());
        data.put("totalCount", logs.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

