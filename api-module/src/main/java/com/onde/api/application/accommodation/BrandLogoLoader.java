package com.onde.api.application.accommodation;

import java.io.InputStream;

/**
 * PDF 리포트용 ONDE 로고를 classpath에서만 로드합니다.
 * 외부 URL 요청 없이 번들 리소스(/logo.png)를 사용해 SSRF를 방지합니다.
 */
final class BrandLogoLoader {

    private static final String LOGO_CLASSPATH = "/logo.png";

    private BrandLogoLoader() {
    }

    static byte[] loadLogoBytes(Class<?> anchorClass) {
        try (InputStream inputStream = anchorClass.getResourceAsStream(LOGO_CLASSPATH)) {
            if (inputStream == null) {
                return null;
            }
            return inputStream.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }
}
