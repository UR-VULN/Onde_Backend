package com.onde.api.application.notification.dto;

import com.onde.core.entity.notification.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;

    @NotNull(message = "기기 타입은 필수입니다.")
    private DeviceType deviceType;
}
