package com.onde.core.entity.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor
@RedisHash(value = "auth:refresh")
public class RefreshToken {

    @Id
    private String subjectId;

    private String refreshToken;

    private String clientIp;

    private String clientUserAgentHash;

    private String activeAccessJti;

    @TimeToLive
    private Long expiration;

    public RefreshToken(
            String subjectId,
            String refreshToken,
            String clientIp,
            String clientUserAgentHash,
            String activeAccessJti,
            Long expiration
    ) {
        this.subjectId = subjectId;
        this.refreshToken = refreshToken;
        this.clientIp = clientIp;
        this.clientUserAgentHash = clientUserAgentHash;
        this.activeAccessJti = activeAccessJti;
        this.expiration = expiration;
    }

    public void updateActiveAccess(String activeAccessJti, String clientIp, String clientUserAgentHash) {
        this.activeAccessJti = activeAccessJti;
        this.clientIp = clientIp;
        this.clientUserAgentHash = clientUserAgentHash;
    }
}
