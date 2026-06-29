package com.onde.core.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component("mariadb") 
public class MariaDbHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public MariaDbHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            // isValid(1)은 1초 내에 연결이 유효한지 확인
            // JDBC 드라이버 차원에서 오버헤드 없이 연결 유효성만 가볍게 확인
            if (conn.isValid(1)) {
                return Health.up().withDetail("database", "MariaDB is online").build();
            }
        } catch (SQLException e) {
            return Health.down().withDetail("error", "MariaDB connection failed: " + e.getMessage()).build();
        }
        return Health.down().withDetail("error", "MariaDB connection is invalid").build();
    }
}
