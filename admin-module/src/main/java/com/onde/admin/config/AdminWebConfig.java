package com.onde.admin.config;

import com.onde.admin.security.LoginAdminArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

/**
 * MVC 설정 — CORS는 {@link com.onde.core.config.CorsConfigurationSupport} + AdminSecurityConfig 전용.
 * 27.pdf: {@code WebMvcConfigurer.addCorsMappings} / {@code allowedOriginPatterns("*")} 사용 금지.
 */
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginAdminArgumentResolver());
    }
}
