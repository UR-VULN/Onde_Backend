package com.onde.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CORS 허용 Origin — 운영 기본값에는 localhost 미포함, {@code onde.cors.allow-localhost=true} 일 때만 개발 Origin 추가.
 * 전체 와일드카드({@code *})는 허용하지 않으며, 운영은 {@code https://*.onde.click} 등 신뢰 도메인 패턴만 사용합니다.
 */
@ConfigurationProperties(prefix = "onde.cors")
public class CorsOriginProperties {

    private boolean allowLocalhost = false;

    private List<String> productionOriginPatterns = List.of("https://*.onde.click");

    private List<String> localOrigins = List.of(
            "http://localhost:5173",
            "http://localhost:3000");

    public boolean isAllowLocalhost() {
        return allowLocalhost;
    }

    public void setAllowLocalhost(boolean allowLocalhost) {
        this.allowLocalhost = allowLocalhost;
    }

    public List<String> getProductionOriginPatterns() {
        return productionOriginPatterns;
    }

    public void setProductionOriginPatterns(List<String> productionOriginPatterns) {
        this.productionOriginPatterns = productionOriginPatterns;
    }

    public List<String> getLocalOrigins() {
        return localOrigins;
    }

    public void setLocalOrigins(List<String> localOrigins) {
        this.localOrigins = localOrigins;
    }

    /**
     * Spring Security CORS에 적용할 최종 Origin 패턴 목록.
     */
    public List<String> getAllowedOriginPatterns() {
        Set<String> merged = new LinkedHashSet<>(productionOriginPatterns);
        if (allowLocalhost) {
            merged.addAll(localOrigins);
        }
        List<String> patterns = List.copyOf(merged);
        assertNoUnsafeWildcard(patterns);
        return patterns;
    }

    public boolean containsLocalhostOrigin() {
        return getAllowedOriginPatterns().stream().anyMatch(CorsOriginProperties::isLocalhostOrigin);
    }

    private static void assertNoUnsafeWildcard(List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalStateException("CORS Origin 패턴은 비어 있을 수 없습니다.");
            }
            if ("*".equals(pattern.trim())) {
                throw new IllegalStateException("CORS 전체 와일드카드(*)는 허용되지 않습니다.");
            }
            if (pattern.startsWith("*") || pattern.contains("://*") && !pattern.contains("*.")) {
                throw new IllegalStateException("CORS Origin 패턴이 과도하게 개방되어 있습니다: " + pattern);
            }
        }
    }

    private static boolean isLocalhostOrigin(String origin) {
        if (origin == null) {
            return false;
        }
        String normalized = origin.toLowerCase();
        return normalized.startsWith("http://localhost:")
                || normalized.startsWith("https://localhost:")
                || normalized.startsWith("http://127.0.0.1:")
                || normalized.startsWith("https://127.0.0.1:");
    }
}
