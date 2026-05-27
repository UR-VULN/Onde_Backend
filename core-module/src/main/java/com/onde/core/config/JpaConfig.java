package com.onde.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // BaseTimeEntity의 생성일/수정일 자동 기록 기능을 활성화합니다.
public class JpaConfig {
    // 향후 쿼리 성능 최적화를 위한 설정이나 기타 JPA 관련 빈(Bean) 등록이 필요할 때 이곳에 추가합니다.
}