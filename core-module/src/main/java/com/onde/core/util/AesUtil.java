package com.onde.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class AesUtil {

    private final SecretKeySpec secretKey;
    private final IvParameterSpec iv;

    public AesUtil(@Value("${encryption.aes.secret-key:ThisIsLocalTestSecretKey12345678}") String key) {
        // 32바이트 키 생성
        this.secretKey = new SecretKeySpec(key.getBytes(), "AES");
        // CBC 모드에 필요한 16바이트 IV (여기서는 키의 앞 16자리 사용)
        this.iv = new IvParameterSpec(key.substring(0, 16).getBytes());
    }

    public String encrypt(String text) {
        if (text == null)
            return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("암호화 중 오류가 발생했습니다.", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null)
            return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("복호화 중 오류가 발생했습니다.", e);
        }
    }
}