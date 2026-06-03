package com.onde.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 정산 스케줄러(SettlementScheduler) 동작을 위해 스케줄링 기능을 활성화합니다.
// 1. 스프링 빈 스캔 범위 지정
@SpringBootApplication(scanBasePackages = {"com.onde.api", "com.onde.core"})
// 2. JPA 리포지토리 스캔 범위 지정
@EnableJpaRepositories(basePackages = {"com.onde.api", "com.onde.core"})
// 3. JPA 엔티티 스캔 범위 지정
@EntityScan(basePackages = {"com.onde.api", "com.onde.core"})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}