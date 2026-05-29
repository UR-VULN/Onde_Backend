package com.onde.api.application.settlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NtsBusinessVerificationService {

    @Value("${nts.api.service-key}")
    private String serviceKey;

    @Value("${nts.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyBusinessNumber(String businessNumber) {

        return true;
        
        // // 하이픈(-) 제거
        // String cleanNumber = businessNumber.replaceAll("-", "");

        // String url = apiUrl + "?serviceKey=" + serviceKey;

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // // 요청 바디 생성 {"b_no": ["1234567890"]}
        // Map<String, List<String>> body = new HashMap<>();
        // body.put("b_no", Collections.singletonList(cleanNumber));

        // HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

        // try {
        //     ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        //     Map<String, Object> responseBody = response.getBody();

        //     if (responseBody != null && responseBody.containsKey("data")) {
        //         List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
        //         if (!dataList.isEmpty()) {
        //             // b_stt_cd 가 "01" 이면 계속사업자(정상)
        //             String statusCd = (String) dataList.get(0).get("b_stt_cd");
        //             return "01".equals(statusCd);
        //         }
        //     }
        //     return false;
        // } catch (Exception e) {
        //     // 통신 장애 시 로그를 남기고 실패 처리
        //     return false;
        // }
    }
} 
    
