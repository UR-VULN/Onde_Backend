package com.onde.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import java.io.InputStream;

/**
 * [보안 강화 - WEB-1] Stored XSS 공격 방어를 위한 HTML Sanitize 컴포넌트
 * OWASP AntiSamy 모듈을 사용하여 태그 화이트리스트 기반의 소독을 수행하고,
 * 오류 시 HtmlUtils.htmlEscape를 Fallback으로 처리합니다.
 */
@Slf4j
@Component
public class XssFilter {
    private Policy policy;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/antisamy-slashdot.xml")) {
            if (is == null) {
                throw new IllegalStateException("antisamy-slashdot.xml 정책 파일을 리소스 경로에서 찾을 수 없습니다.");
            }
            policy = Policy.getInstance(is);
            log.info("AntiSamy Stored XSS policy initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to load AntiSamy policy, fallback to htmlEscape will be used.", e);
        }
    }

    /**
     * 입력 텍스트를 검사하여 안전한 HTML 형태만 남기고 반환합니다.
     */
    public String clean(String input) {
        if (input == null) return null;
        if (policy == null) {
            return HtmlUtils.htmlEscape(input);
        }
        try {
            AntiSamy as = new AntiSamy();
            CleanResults cr = as.scan(input, policy);
            return cr.getCleanHTML();
        } catch (Exception e) {
            log.warn("AntiSamy scanning failed, fallback to HtmlUtils.htmlEscape. Input: {}", input, e);
            return HtmlUtils.htmlEscape(input);
        }
    }
}
