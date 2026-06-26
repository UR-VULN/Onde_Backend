package com.onde.core.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 운영 프로필에서 localhost CORS가 켜져 있으면 기동을 중단합니다.
 */
@Component
public class CorsOriginStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final CorsOriginProperties corsOriginProperties;
    private final Environment environment;

    public CorsOriginStartupValidator(CorsOriginProperties corsOriginProperties, Environment environment) {
        this.corsOriginProperties = corsOriginProperties;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (!corsOriginProperties.containsLocalhostOrigin()) {
            return;
        }
        if (isProductionProfile()) {
            throw new IllegalStateException(
                    "운영(prod) 환경에서는 localhost CORS Origin을 허용할 수 없습니다. "
                            + "onde.cors.allow-localhost=false 로 설정하세요.");
        }
    }

    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }
}
