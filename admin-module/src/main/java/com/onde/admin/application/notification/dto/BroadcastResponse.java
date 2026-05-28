package com.onde.admin.application.notification.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastResponse {
    private int sentCount;
    private LocalDateTime sentAt;
}
