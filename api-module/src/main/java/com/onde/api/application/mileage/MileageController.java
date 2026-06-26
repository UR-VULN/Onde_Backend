package com.onde.api.application.mileage;

import com.onde.api.application.membergrade.MemberGradeService;
import com.onde.api.security.LoginMember;
import com.onde.core.entity.payment.MileageLog;
import com.onde.core.support.ApiResponse;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/members/me/mileage")
@RequiredArgsConstructor
public class MileageController {

    private final MileageService mileageService;
    private final MemberGradeService memberGradeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMileageSummary(@LoginMember Long userId) {
        int balance = mileageService.getCurrentMileage(userId);
        Map<String, Object> gradeInfo = memberGradeService.getMemberGradeInfo(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("memberId", userId);
        data.put("memberGrade", gradeInfo.get("grade"));
        data.put("mileageBalance", balance);
        data.put("accumulationRate", gradeInfo.get("rate"));
        data.put("nextGradeThreshold", gradeInfo.get("threshold"));

        return ResponseEntity.ok(ApiResponse.success(data, "마일리지 조회 성공"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMileageHistory(
            @LoginMember Long userId,
            @RequestParam(name = "page", defaultValue = "0") @Min(ValidationLimits.PAGE_MIN) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(ValidationLimits.PAGE_SIZE_MIN) @Max(ValidationLimits.PAGE_SIZE_MAX) int size) {

        Page<MileageLog> logs = mileageService.getMileageHistory(userId, PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("logs", logs.getContent());
        data.put("totalCount", logs.getTotalElements());
        data.put("page", page);
        data.put("size", size);

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
