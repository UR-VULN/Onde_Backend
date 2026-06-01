package com.onde.admin.application.dashboard;

import com.onde.core.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 일반 운영 지표 요약
     */
    @GetMapping("/operational")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOperational() {
        Map<String, Object> data = adminDashboardService.getOperationalMetrics();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 전사 총매출 대시보드 요약
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(name = "month") String month) {
        
        Map<String, Object> data = adminDashboardService.getSummary(month);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 도메인별 매출 비중 차트
     */
    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCharts(
            @RequestParam(name = "month") String month) {
        
        Map<String, Object> data = adminDashboardService.getCharts(month);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

