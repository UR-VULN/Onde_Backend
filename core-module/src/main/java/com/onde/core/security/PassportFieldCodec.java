package com.onde.core.security;

import com.onde.core.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 여권번호 저장(암호화) 및 권한 있는 조회(복호화)·일반 노출(마스킹).
 */
@Component
@RequiredArgsConstructor
public class PassportFieldCodec {

    private final AesUtil aesUtil;

    public String encryptForStorage(String plainPassport) {
        if (plainPassport == null) {
            return null;
        }
        return aesUtil.encrypt(plainPassport);
    }

    public String decryptForAuthorizedRead(String storedPassport) {
        if (storedPassport == null || storedPassport.isBlank()) {
            return "";
        }
        try {
            return aesUtil.decrypt(storedPassport);
        } catch (RuntimeException ex) {
            return storedPassport;
        }
    }

    public String maskForDisplay(String storedPassport) {
        return PersonalDataMasker.maskPassport(decryptForAuthorizedRead(storedPassport));
    }
}
