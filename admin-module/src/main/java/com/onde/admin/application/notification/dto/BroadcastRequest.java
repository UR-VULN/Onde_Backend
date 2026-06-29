package com.onde.admin.application.notification.dto;

import com.onde.core.entity.member.MemberRole;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastRequest {

    @NotBlank(message = "공지 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "공지 본문은 필수입니다.")
    private String body;

    private boolean targetAll;

    private List<MemberRole> targetRoles;
}
