package com.onde.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * [보안 강화 - WEB-3 / ADMIN-2] SSRF (Server-Side Request Forgery) 방어 검증기
 * 아웃바운드 HTTP 요청을 전송하기 전에 외부 URL 대상 호스트에 대하여 
 * 스키마 화이트리스트, 도메인 화이트리스트 및 내부망/루프백 IP 검증을 수행합니다.
 */
@Slf4j
@Component
public class UrlValidator {
    // 허용된 도메인 리스트 (화이트리스트)
    private static final Set<String> ALLOWED_DOMAINS = Set.of(
        "cdn.onde.click", "s3.amazonaws.com", "storage.onde.click", "onde.click"
    );

    // 차단할 위험한 스키마 프로토콜 목록
    private static final Set<String> BLOCKED_SCHEMES = Set.of(
        "file", "gopher", "dict", "ftp", "ldap", "jar"
    );

    /**
     * URL이 허용된 사외 안전 도메인 및 정상 스키마인지 검사하고 내부 IP 접근인 경우 예외를 던집니다.
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            
            // 1. 위험한 프로토콜 스키마 검증
            if (scheme != null && BLOCKED_SCHEMES.contains(scheme.toLowerCase())) {
                throw new SecurityException("보안 정책상 허용되지 않는 URL 프로토콜 스키마입니다: " + scheme);
            }
            
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new SecurityException("유효하지 않은 호스트 주소입니다.");
            }
            
            // 2. 도메인 화이트리스트 검사 (끝부분 매칭 포함)
            boolean isAllowedDomain = ALLOWED_DOMAINS.stream()
                    .anyMatch(domain -> host.equalsIgnoreCase(domain) || host.toLowerCase().endsWith("." + domain));
                    
            if (!isAllowedDomain) {
                log.warn("SSRF 차단: 허용되지 않는 사외 도메인 접근 감지 - {}", host);
                throw new SecurityException("허용되지 않는 외부 도메인 주소입니다.");
            }
            
            // 3. DNS Lookup을 통한 내부망(Local) IP 기습 침투 차단
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                log.warn("SSRF 차단: 로컬/내부망 대역 IP로의 요청 시도 감지 - {} ({})", host, addr.getHostAddress());
                throw new SecurityException("보안 정책상 내부망 IP로의 아웃바운드 요청은 금지됩니다.");
            }
            
        } catch (URISyntaxException e) {
            log.error("SSRF URL 문법 파싱 오류: {}", url, e);
            throw new SecurityException("유효한 URL 포맷 형식이 아닙니다.");
        } catch (UnknownHostException e) {
            log.error("SSRF 호스트 IP 분석(DNS Lookup) 실패: {}", url, e);
            throw new SecurityException("도메인의 IP 정보를 확인할 수 없거나 유효하지 않습니다.");
        }
    }
}
