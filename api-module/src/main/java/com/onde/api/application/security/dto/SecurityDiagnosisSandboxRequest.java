package com.onde.api.application.security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityDiagnosisSandboxRequest {
    private String template;
    private String logoUrl;
}
