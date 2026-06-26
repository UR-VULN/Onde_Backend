package com.onde.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HttpOnly 인증 쿠키 속성.
 * 운영: Secure + SameSite=None. local/dev: HTTP localhost에서도 쿠키가 저장되도록 완화합니다.
 */
@ConfigurationProperties(prefix = "onde.auth.cookie")
public class AuthCookieProperties {

    private boolean secure = true;
    private String sameSite = "None";

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }
}
