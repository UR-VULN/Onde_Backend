package com.onde.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * [Day 9] Redisson 분산 락 라이프사이클 실행기 (Dynamic Fallback 모의 우회 기동 포함)
     */
    public <T> T executeWithLock(String key, long waitTimeSeconds, long leaseTimeSeconds, Callable<T> callback) {
        // 로컬 개발 환경 및 Redis 미가동 시 Dynamic Fallback 작동 (무장애 모의 락 실행 우회)
        if (redissonClient == null) {
            log.warn("⚠️ [DISTRIBUTED LOCK FALLBACK] RedissonClient is not initialized. 우회하여 비즈니스 로직을 동적으로 직접 실행합니다.");
            try {
                return callback.call();
            } catch (Exception e) {
                throw new RuntimeException("비즈니스 콜백 실행 중 오류 발생", e);
            }
        }

        RLock lock = redissonClient.getLock(key);
        boolean isAcquired = false;

        try {
            log.info("🔒 Attempting to acquire Redis distributed lock for key: {}", key);
            isAcquired = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);

            if (!isAcquired) {
                log.warn("❌ [LOCK TIMEOUT] Failed to acquire distributed lock for key: {}", key);
                throw new com.onde.core.exception.ValidationException(com.onde.core.exception.ErrorCode.SEAT_SOLD_OUT);
            }

            log.info("🔒 Acquired distributed lock successfully for key: {}", key);
            return callback.call();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("분산 락 획득 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            throw new RuntimeException("분산 락 트랜잭션 비즈니스 로직 실행 중 예외 발생", e);
        } finally {
            if (isAcquired) {
                try {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("🔓 Released distributed lock successfully for key: {}", key);
                    }
                } catch (Exception e) {
                    log.error("🔓 [LOCK RELEASE ERROR] Failed to release lock: {}", e.getMessage());
                }
            }
        }
    }
}
