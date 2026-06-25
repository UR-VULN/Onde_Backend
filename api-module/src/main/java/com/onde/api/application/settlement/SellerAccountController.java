package com.onde.api.application.settlement;

import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.support.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 판매자 정산 계좌 및 사업자 정보 관리를 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/seller/account")
@RequiredArgsConstructor
public class SellerAccountController {

    private final NtsBusinessVerificationService ntsBusinessVerificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String BAN_PREFIX = "BAN:SELLER_VERIFY:";

    // IP별 버킷을 관리하기 위한 맵 (분당 3회 제한)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
                // greedy(20초마다 1개씩 충전) 대신 intervally(1분 뒤에 3개 한꺼번에 충전) 사용
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * 판매자 사업자등록번호의 진위 여부를 국세청 API를 통해 검증합니다.
     *
     * @param request     사업자등록번호, 대표자명, 개업일자를 담은 DTO
     * @param httpRequest 클라이언트 IP 추출을 위한 HttpServletRequest
     * @return 검증 성공 여부 및 결과 (true/false)
     */
    @PostMapping("/verify-business")
    public ResponseEntity<ApiResponse<VerifyBusinessResponse>> verifyBusiness(
            @RequestBody VerifyBusinessRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
        }

        // 1. Redis 블랙리스트(밴) 확인
        String banKey = BAN_PREFIX + clientIp;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            System.out.println("[RATE LIMIT DEBUG] BANNED IP REJECTED: " + clientIp);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        Bucket bucket = resolveBucket(clientIp);
        long availableTokens = bucket.getAvailableTokens();
        System.out.println("[RATE LIMIT DEBUG] IP: " + clientIp + ", Controller Hash: " + this.hashCode() + ", Buckets size: " + buckets.size() + ", Available Tokens before consume: " + availableTokens);

        if (!bucket.tryConsume(1)) {
            System.out.println("[RATE LIMIT DEBUG] BLOCKED IP (Added to Ban list for 1 hour): " + clientIp);
            // 2. 제한을 초과한 경우 Redis에 1시간 동안 IP 등록 (블랙리스트)
            redisTemplate.opsForValue().set(banKey, "BANNED", 1, TimeUnit.HOURS);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        System.out.println("[RATE LIMIT DEBUG] CONSUMED IP: " + clientIp);

        NtsBusinessVerificationService.BusinessVerificationResult result =
                ntsBusinessVerificationService.verifyBusiness(
                        request.getBusinessNumber(),
                        request.getRepresentativeName(),
                        request.getOpenDate()
                );
        VerifyBusinessResponse response = VerifyBusinessResponse.builder()
                .verified(result.verified())
                .businessStatusCode(result.businessStatusCode())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, result.message()));
    }

    /**
     * 사업자 진위 확인 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessRequest {
        private String businessNumber;      // 사업자등록번호
        private String representativeName;   // 대표자성명
        private String openDate;             // 개업일자 (YYYYMMDD)
    }

    /**
     * 사업자 진위 확인 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyBusinessResponse {
        private boolean verified;            // 검증 완료 여부
        private String businessStatusCode;   // 계속사업자(01), 휴업(02), 폐업(03)
    }
}
