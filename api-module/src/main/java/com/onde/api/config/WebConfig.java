package com.onde.api.config;

import com.onde.api.security.LoginMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

/**
 * MVC 설정 — CORS는 {@link com.onde.core.config.CorsConfigurationSupport} + SecurityConfig 전용.
 * 27.pdf: {@code WebMvcConfigurer.addCorsMappings} / {@code allowedOriginPatterns("*")} 사용 금지.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
