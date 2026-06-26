package com.onde.api.application.security;

import com.onde.api.application.security.dto.SecurityDiagnosisSandboxRequest;
import com.onde.api.application.security.dto.SecurityDiagnosisSandboxResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;

/**
 * 모의침투/진단 전용 LFI·SSRF 샌드박스 API.
 * 운영 환경에서는 security.diagnosis.sandbox.enabled=false 로 비활성화합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/security-diagnosis")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.diagnosis.sandbox.enabled", havingValue = "true")
public class SecurityDiagnosisSandboxController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/sandbox")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SecurityDiagnosisSandboxResponse runSandbox(@RequestBody SecurityDiagnosisSandboxRequest req) {
        SecurityDiagnosisSandboxResponse response = new SecurityDiagnosisSandboxResponse();

        if (req.getTemplate() != null && !req.getTemplate().isBlank()
                && !"verification".equals(req.getTemplate())
                && !"business".equals(req.getTemplate())) {
            File file = new File("/app", req.getTemplate());
            if (file.exists() && file.isFile()) {
                try {
                    response.setLfiResult(new String(Files.readAllBytes(file.toPath())));
                } catch (Exception e) {
                    response.setLfiResult("Failed to read file: " + e.getMessage());
                }
            } else {
                response.setLfiResult("Template not found at: " + file.getAbsolutePath());
            }
        }

        if (req.getLogoUrl() != null && !req.getLogoUrl().isBlank()
                && !"https://onde.click/assets/logo.png".equals(req.getLogoUrl())) {
            try {
                String ssrfResponse = restTemplate.getForObject(req.getLogoUrl(), String.class);
                if (ssrfResponse != null) {
                    response.setSsrfResult(ssrfResponse.substring(0, Math.min(500, ssrfResponse.length())));
                } else {
                    response.setSsrfResult("Empty response");
                }
            } catch (Exception e) {
                response.setSsrfResult("Failed: " + e.getMessage());
            }
        }

        return response;
    }
}
