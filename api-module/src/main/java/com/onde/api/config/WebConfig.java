package com.onde.api.config;

import com.onde.api.security.LoginMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // [이행진단 1, 2] 개발 편의상 사용하던 와일드카드(*) 대신 명시적 Origin 화이트리스트 적용
                .allowedOrigins(
                        "http://localhost:5173", 
                        "http://localhost:3000", 
                        "https://onde.click", 
                        "https://www.onde.click", 
                        "https://admin.onde.click"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // [이행진단 1] 와일드카드(*) 대신 명시적 Allowed Headers 적용
                .allowedHeaders(
                        "Authorization", 
                        "Content-Type", 
                        "Accept", 
                        "Origin", 
                        "X-Requested-With", 
                        "X-XSRF-TOKEN", 
                        "X-CSRF-TOKEN",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                )
                .allowCredentials(true);
    }
}
