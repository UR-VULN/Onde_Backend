package com.onde.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsOriginPropertiesTest {

    @Test
    @DisplayName("운영 기본값에는 localhost Origin이 포함되지 않는다")
    void productionDefaults_excludeLocalhost() {
        CorsOriginProperties properties = new CorsOriginProperties();

        assertThat(properties.getAllowedOriginPatterns())
                .containsExactly("https://*.onde.click");
        assertThat(properties.containsLocalhostOrigin()).isFalse();
    }

    @Test
    @DisplayName("allow-localhost=true 일 때만 개발 Origin이 추가된다")
    void allowLocalhost_includesDevOrigins() {
        CorsOriginProperties properties = new CorsOriginProperties();
        properties.setAllowLocalhost(true);

        assertThat(properties.getAllowedOriginPatterns())
                .contains(
                        "https://*.onde.click",
                        "http://localhost:5173",
                        "http://localhost:3000");
        assertThat(properties.containsLocalhostOrigin()).isTrue();
    }

    @Test
    @DisplayName("전체 와일드카드 CORS 패턴은 거부된다")
    void rejectsGlobalWildcard() {
        CorsOriginProperties properties = new CorsOriginProperties();
        properties.setProductionOriginPatterns(new java.util.ArrayList<>(java.util.List.of("*")));

        assertThatThrownBy(properties::getAllowedOriginPatterns)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("*");
    }
}
