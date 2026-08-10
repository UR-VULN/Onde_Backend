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
     * Redisson 분산 락을 획득한 뒤 콜백을 실행하고, 완료 여부와 무관하게 락을 해제합니다.
     * 대기 시간 내에 락을 얻지 못하면 SEAT_SOLD_OUT 예외로 처리합니다.
     */
    public <T> T executeWithLock(String key, long waitTimeSeconds, long leaseTimeSeconds, Callable<T> callback) {
        // Redis를 띄우지 않는 로컬 환경에서도 기동되도록, RedissonClient가 없으면 락 없이 콜백만 실행
        // (이 경로에서는 동시성 제어가 적용되지 않으므로 운영 환경에서는 Redis 연결이 필수)
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
        } catch (com.onde.core.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [LOCK EXECUTION ERROR] Exception inside lock execution callback: {}", e.getMessage(), e);
            throw new RuntimeException("분산 락 트랜잭션 비즈니스 로직 실행 중 예외 발생: " + e.getMessage(), e);
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
