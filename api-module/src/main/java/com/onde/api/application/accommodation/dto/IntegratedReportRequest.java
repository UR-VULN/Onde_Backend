package com.onde.api.application.accommodation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegratedReportRequest {

    @Pattern(regexp = "^(verification|business)$", message = "template은 verification 또는 business만 허용됩니다.")
    @Size(max = 20, message = "template 형식이 올바르지 않습니다.")
    private String template;
}
