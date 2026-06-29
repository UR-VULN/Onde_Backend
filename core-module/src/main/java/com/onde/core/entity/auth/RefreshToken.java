package com.onde.core.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "auth:refresh") // Redis에 저장될 Key의 Prefix 설정
public class RefreshToken {

    @Id
    private String email; // 이메일을 식별자로 사용하여 중복 로그인 관리 용이

    private String refreshToken;

    @TimeToLive
    private Long expiration; // TTL (초 단위, 이 시간이 지나면 자동 삭제됨)
}