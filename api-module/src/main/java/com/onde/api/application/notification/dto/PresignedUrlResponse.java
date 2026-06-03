package com.onde.api.application.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresignedUrlResponse {
    private Long reservationId;
    private String presignedUrl;
    private String fileUrl;
    private String downloadUrl;
}
