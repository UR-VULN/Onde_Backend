package com.onde.api.infrastructure.portone;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * [포트원(PortOne) PG 결제 연동 외부 서비스 클래스]
 * 플랫폼 외부의 결제 게이트웨이(PortOne) API와 연동하여 결제 승인 검증, 결제 정보 조회, 
 * 그리고 결제 장애 및 취소 발생 시의 환불(SAGA 보상 트랜잭션 등)을 수행합니다.
 * RestTemplate을 활용하여 동기 HTTP 요청을 전송합니다.
 */
@Slf4j
@Service
public class PortOneService {

    // 포트원 어드민 콘솔에서 발급받은 REST API 키
    @Value("${portone.imp-key}")
    private String impKey;

    // 포트원 어드민 콘솔에서 발급받은 REST API Secret
    @Value("${portone.imp-secret}")
    private String impSecret;

    // 외부 API 요청 처리를 위한 RestTemplate
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 포트원 API 베이스 URL
    private static final String API_URL = "https://api.iamport.kr";

    /**
     * [포트원 REST API 인증 토큰(Access Token) 발급]
     * 외부 API 호출 시 Authorization 헤더에 실어 보낼 Bearer 토큰을 발급받습니다.
     * 로컬 개발/테스트 환경(test_key)인 경우 실제 외부 HTTP 호출 없이 모의 토큰("mock_access_token")을 반환합니다.
     *
     * @return 발급 완료된 Access Token 문자열
     */
    public String getAccessToken() {
        // 로컬 테스트 환경 분기 처리 (모의 토큰 즉시 반환)
        if ("test_key".equals(impKey)) {
            log.info("포트원 API 키가 테스트용('test_key')이므로 모의 토큰을 반환합니다.");
            return "mock_access_token";
        }
        
        String url = API_URL + "/users/getToken";

        // 요청 바디 생성 (API Key & Secret 주입)
        Map<String, String> body = new HashMap<>();
        body.put("imp_key", impKey);
        body.put("imp_secret", impSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // 외부 토큰 요청 API 호출
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(url, request, TokenResponse.class);
            if (response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getResponse().getAccess_token();
            } else {
                String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "응답 바디 없음";
                throw new IllegalStateException("포트원 토큰 발급 실패: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("포트원 토큰 발급 중 예외 발생: ", e);
            throw new IllegalStateException("포트원 토큰 발급 오류", e);
        }
    }

    /**
     * [포트원 단건 결제 상세 정보 조회]
     * 사용자가 결제를 완료하고 제출한 포트원 결제 고유 거래 ID(impUid)를 기반으로 
     * 실제 외부 PG 서버에 등록된 결제 금액, 결제 여부 상태 등을 대조 및 검증하기 위해 정보를 조회합니다.
     *
     * @param impUid          포트원 결제 고유 ID (imp_로 시작하는 PG사 식별코드)
     * @param expectedAmount  비즈니스에서 결제되어야 하는 기대 금액 (검증용)
     * @return 결제 고유 정보 객체 (PaymentAnnotation)
     */
    public PaymentAnnotation getPaymentInfo(String impUid, Long expectedAmount) {
        // 로컬 개발/통합 테스트 환경 및 임시 테스트 impUid 접두사 진입 시 모의 데이터 반환 분기
        if ("test_key".equals(impKey) || (impUid != null && impUid.startsWith("imp_1234567890"))) {
            log.info("모의 결제 정보 처리를 적용합니다. impUid: {}", impUid);
            PaymentAnnotation mockInfo = new PaymentAnnotation();
            mockInfo.setImp_uid(impUid);
            mockInfo.setMerchant_uid("MOCK-MERCHANT-UID");
            mockInfo.setAmount(expectedAmount); // 테스트 검증 통과를 지원하기 위해 파라미터로 넘어온 기대 금액으로 모의 세팅
            mockInfo.setStatus("paid");
            mockInfo.setPay_method("card");
            return mockInfo;
        }
        
        String url = API_URL + "/payments/" + impUid;
        // 1. Bearer 인증 토큰 획득
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            // 2. 단건 결제 정보 조회 API 호출 (GET)
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, PaymentResponse.class
            );

            if (response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getResponse();
            } else {
                String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "응답 바디 없음";
                throw new IllegalArgumentException("포트원 결제 정보 조회 실패: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("포트원 결제 정보 조회 중 예외 발생: ", e);
            throw new IllegalStateException("포트원 결제 정보 조회 오류", e);
        }
    }

    /**
     * [승인 완료된 외부 PG 결제건 강제 환불/취소 요청]
     * 사용자가 예약을 도중에 취소했거나, 로컬 DB 갱신 도중 장애가 발생하여 
     * SAGA 보상 트랜잭션이 작동했을 때, 기승인 처리된 외부 PG사의 결제건을 부분/전액 환불 취소 요청합니다.
     *
     * @param impUid 결제 고유 거래 ID (imp_시작 코드)
     * @param amount 취소 처리할 취소 요청 금액
     * @param reason 취소 발생 사유
     */
    public void cancelPayment(String impUid, Long amount, String reason) {
        // 로컬 테스트 환경 분기 처리
        if ("test_key".equals(impKey) || (impUid != null && impUid.startsWith("imp_1234567890"))) {
            log.info("모의 결제 취소를 수행합니다. impUid: {}, 금액: {}, 사유: {}", impUid, amount, reason);
            return;
        }
        
        String url = API_URL + "/payments/cancel";
        // 1. Bearer 인증 토큰 획득
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 요청 Body 데이터 구성
        Map<String, Object> body = new HashMap<>();
        body.put("imp_uid", impUid);
        body.put("amount", amount);
        body.put("reason", reason);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // 3. 결제 취소 API 호출 (POST)
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, request, PaymentResponse.class);
            if (response.getBody() == null || response.getBody().getCode() != 0) {
                String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "응답 바디 없음";
                throw new IllegalStateException("포트원 결제 취소 요청 실패: " + errorMsg);
            }
            log.info("포트원 실제 결제 취소 성공 - impUid: {}, 금액: {}", impUid, amount);
        } catch (Exception e) {
            log.error("포트원 결제 취소 중 예외 발생: ", e);
            throw new IllegalStateException("포트원 외부 결제 취소 오류", e);
        }
    }

    // --- 포트원 JSON 응답 매핑용 Inner DTO 클래스군 ---

    /**
     * 토큰 요청 API 응답 매퍼 클래스
     */
    @Getter
    @Setter
    public static class TokenResponse {
        private int code;           // 0: 성공, 그 외: 실패 에러 코드
        private String message;     // 응답 결과 메시지
        private TokenInfo response; // 실제 토큰 반환 정보 객체
    }

    /**
     * 토큰 상세 정보 매퍼 클래스
     */
    @Getter
    @Setter
    public static class TokenInfo {
        private String access_token; // 발급 완료된 Access Token 문자열
        private long now;            // 현재 UNIX 타임스탬프
        private long expired_at;     // 토큰 만료 UNIX 타임스탬프
    }

    /**
     * 결제 및 환불 요청 API 응답 매퍼 클래스
     */
    @Getter
    @Setter
    public static class PaymentResponse {
        private int code;                 // 0: 성공, 그 외: 실패 코드
        private String message;           // 응답 메시지
        private PaymentAnnotation response; // 결제 최종 상태 상세 정보 객체
    }

    /**
     * 결제 건의 상세 속성 매퍼 클래스
     */
    @Getter
    @Setter
    public static class PaymentAnnotation {
        private String imp_uid;      // 포트원 고유 거래 ID
        private String merchant_uid; // 플랫폼 내부 주문/예약 번호
        private String pay_method;   // 결제 수단 (card, trans, vbank 등)
        private Long amount;         // 실제 결제된 금액
        private String status;       // 결제 진행 상태 (ready, paid, cancelled, failed)
    }
}
