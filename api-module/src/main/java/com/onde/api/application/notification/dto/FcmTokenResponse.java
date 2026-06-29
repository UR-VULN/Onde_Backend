package com.onde.api.application.notification.dto;

import com.onde.core.entity.notification.DeviceType;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenResponse {
    private Long memberId;
    private DeviceType deviceType;
    private LocalDateTime savedAt;

    public static FcmTokenResponse of(Long memberId, DeviceType deviceType) {
        return FcmTokenResponse.builder()
                .memberId(memberId)
                .deviceType(deviceType)
                .savedAt(LocalDateTime.now())
                .build();
    }
}
