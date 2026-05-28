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


    /**
     * 전사 총매출 대시보드 요약
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(name = "month") String month) {
        
        long totalRevenue = 125000000L;
        
        Map<String, Object> byDomain = new HashMap<>();
        byDomain.put("flight", 75000000L);
        byDomain.put("accommodation", 32000000L);
        byDomain.put("car", 10000000L);
        byDomain.put("insurance", 8000000L);

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("totalBookings", 1284);
        data.put("pendingSettlements", 8);
        data.put("byDomain", byDomain);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 도메인별 매출 비중 차트
     */
    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCharts(
            @RequestParam(name = "month") String month) {
        
        java.util.List<Map<String, Object>> segments = new java.util.ArrayList<>();
        
        Map<String, Object> flight = new HashMap<>();
        flight.put("domain", "항공");
        flight.put("amount", 75000000L);
        flight.put("ratio", 0.60);
        segments.add(flight);

        Map<String, Object> hotel = new HashMap<>();
        hotel.put("domain", "숙소");
        hotel.put("amount", 32000000L);
        hotel.put("ratio", 0.256);
        segments.add(hotel);

        Map<String, Object> car = new HashMap<>();
        car.put("domain", "렌터카");
        car.put("amount", 10000000L);
        car.put("ratio", 0.08);
        segments.add(car);

        Map<String, Object> ins = new HashMap<>();
        ins.put("domain", "보험");
        ins.put("amount", 8000000L);
        ins.put("ratio", 0.064);
        segments.add(ins);

        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "PIE");
        data.put("segments", segments);

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
