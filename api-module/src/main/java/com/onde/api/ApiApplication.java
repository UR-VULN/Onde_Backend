package com.onde.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableScheduling // 정산 스케줄러(SettlementScheduler) 동작을 위해 스케줄링 기능을 활성화합니다.
// 1. 일반 Bean(Service, Component, Config 등) 스캔 영역
@SpringBootApplication(scanBasePackages = { "com.onde.api", "com.onde.core" })
// 2. DB 테이블(Entity) 클래스 스캔 영역 지정
@EntityScan(basePackages = { "com.onde.core" })
// 3. DB 통신(Repository) 인터페이스 스캔 영역 지정
@EnableJpaRepositories(basePackages = { "com.onde.core" })

public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
