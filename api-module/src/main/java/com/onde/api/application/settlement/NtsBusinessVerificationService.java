package com.onde.api.application.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NtsBusinessVerificationService {

    @Value("${nts.api.service-key:}")
    private String serviceKey;

    @Value("${nts.api.url:https://api.odcloud.kr/api/nts-businessman/v1/validate}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 국세청 진위확인 API를 통해 사업자등록번호, 대표자명, 개업일자의 일치 여부를 확인합니다.
     * @param businessNumber 사업자등록번호 (하이픈 유무 상관없음)
     * @param representativeName 대표자명
     * @param openDate 개업일자 (YYYYMMDD)
     * @return 국세청 등록 정보와 일치하면 verified=true
     */
    public BusinessVerificationResult verifyBusiness(String businessNumber, String representativeName, String openDate) {
        String cleanNumber = digitsOnly(businessNumber);
        String cleanOpenDate = digitsOnly(openDate);
        String cleanRepresentativeName = representativeName == null ? "" : representativeName.trim();

        if (cleanNumber.length() != 10 || cleanOpenDate.length() != 8 || cleanRepresentativeName.isBlank()) {
            return BusinessVerificationResult.invalid("사업자등록번호, 대표자명, 개업일자를 모두 정확히 입력해 주세요.");
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            return BusinessVerificationResult.invalid("사업자 진위확인 서비스 키가 설정되어 있지 않습니다.");
        }

        String url = buildValidateUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> business = new HashMap<>();
        business.put("b_no", cleanNumber);
        business.put("start_dt", cleanOpenDate);
        business.put("p_nm", cleanRepresentativeName);

        Map<String, List<Map<String, Object>>> body = new HashMap<>();
        body.put("businesses", Collections.singletonList(business));

        HttpEntity<Map<String, List<Map<String, Object>>>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    Map<String, Object> item = dataList.get(0);
                    String valid = String.valueOf(item.getOrDefault("valid", ""));
                    String message = String.valueOf(item.getOrDefault("valid_msg", ""));
                    Map<String, Object> status = item.get("status") instanceof Map
                            ? (Map<String, Object>) item.get("status")
                            : Collections.emptyMap();
                    String businessStatusCode = String.valueOf(status.getOrDefault("b_stt_cd", ""));
                    if ("01".equals(valid)) {
                        if ("02".equals(businessStatusCode)) {
                            return BusinessVerificationResult.invalid("휴업 상태인 사업자입니다.", businessStatusCode);
                        }
                        if ("03".equals(businessStatusCode)) {
                            return BusinessVerificationResult.invalid("폐업한 사업자입니다.", businessStatusCode);
                        }
                        return BusinessVerificationResult.valid(
                                message.isBlank() ? "사업자 진위 확인에 성공하였습니다." : message,
                                businessStatusCode
                        );
                    }
                    return BusinessVerificationResult.invalid(
                            message.isBlank() ? "사업자 정보가 국세청 등록정보와 일치하지 않습니다." : message,
                            businessStatusCode
                    );
                }
            }
            return BusinessVerificationResult.invalid("사업자 진위확인 응답을 확인할 수 없습니다.");
        } catch (Exception e) {
            log.warn("Failed to verify business information with NTS API: {}", e.getMessage());
            return BusinessVerificationResult.invalid("사업자 진위확인 서비스 호출에 실패했습니다.");
        }
    }

    /**
     * 기존 호출부 호환용. 대표자명/개업일자 없는 상태조회는 진위확인이 아니므로 false로 처리한다.
     */
    public boolean verifyBusinessNumber(String businessNumber) {
        return false;
    }

    public boolean verifyBusinessNumber(String businessNumber, String representativeName, String openDate) {
        return verifyBusiness(businessNumber, representativeName, openDate).verified();
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String buildValidateUrl() {
        String delimiter = apiUrl.contains("?") ? "&" : "?";
        String encodedKey = URLEncoder.encode(serviceKey.trim(), StandardCharsets.UTF_8);
        return apiUrl + delimiter + "serviceKey=" + encodedKey + "&returnType=JSON";
    }

    public record BusinessVerificationResult(boolean verified, String message, String businessStatusCode) {
        public static BusinessVerificationResult valid(String message) {
            return valid(message, "");
        }

        public static BusinessVerificationResult valid(String message, String businessStatusCode) {
            return new BusinessVerificationResult(true, message, businessStatusCode);
        }

        public static BusinessVerificationResult invalid(String message) {
            return invalid(message, "");
        }

        public static BusinessVerificationResult invalid(String message, String businessStatusCode) {
            return new BusinessVerificationResult(false, message, businessStatusCode);
        }
    }
}
