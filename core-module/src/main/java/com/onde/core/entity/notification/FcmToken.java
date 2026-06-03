package com.onde.core.entity.notification;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

/**
 * FCM(Firebase Cloud Messaging) 토큰 관리 엔티티입니다.
 * 사용자별 기기 토큰을 저장하여 푸시 알림 발송에 사용합니다.
 */
@Entity
@Table(name = "fcm_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FcmToken extends BaseEntity {

    /**
     * 토큰 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림 토큰 소유 회원 (FK → members.id)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * Firebase SDK에서 수집된 기기 고유 토큰
     */
    @Column(name = "fcm_token", nullable = false, length = 300)
    private String fcmToken;

    /**
     * WEB / ANDROID / IOS 클라이언트 플랫폼 분류
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20, columnDefinition = "VARCHAR(20)")
    private DeviceType deviceType;

    /**
     * 기존 토큰 정보를 갱신합니다.
     */
    public void updateToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
