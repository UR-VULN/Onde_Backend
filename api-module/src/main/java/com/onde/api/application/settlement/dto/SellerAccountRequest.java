package com.onde.api.application.settlement.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerAccountRequest {

    @NotBlank(message = "은행명은 필수입니다.")
    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "은행명은 500자 이하여야 합니다.")
    private String bankName;

    @Size(max = ValidationLimits.NAME_MAX, message = "상호명은 100자 이하여야 합니다.")
    private String businessName;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 연락처 형식이 아닙니다.")
    @Size(max = ValidationLimits.PHONE_MAX, message = "연락처는 20자 이하여야 합니다.")
    private String contactPhone;

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "사업장 주소는 500자 이하여야 합니다.")
    private String businessAddress;

    @Size(max = ValidationLimits.BANK_ACCOUNT_MAX, message = "계좌번호는 30자 이하여야 합니다.")
    private String accountNumber;

    @Size(max = ValidationLimits.NAME_MAX, message = "예금주명은 100자 이하여야 합니다.")
    private String accountHolder;

    @Size(max = ValidationLimits.BUSINESS_NUMBER_MAX, message = "사업자등록번호 형식이 올바르지 않습니다.")
    private String businessNumber;

    @Size(max = ValidationLimits.NAME_MAX, message = "대표자명은 100자 이하여야 합니다.")
    private String representativeName;

    @Pattern(regexp = "^\\d{8}$", message = "개업일자는 YYYYMMDD 형식이어야 합니다.")
    private String openedAt;
}
