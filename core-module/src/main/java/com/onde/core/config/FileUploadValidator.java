package com.onde.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * [보안 강화 - ADMIN-4] 악성 파일 업로드 공격 방어를 위한 업로드 검증기
 * 업로드된 파일의 확장자를 체크하고, Content-Type 위장을 방어하기 위해
 * 파일의 실제 Magic Byte를 읽어 파일 유형의 정합성을 최종 판정합니다.
 */
@Slf4j
@Component
public class FileUploadValidator {
    // 각 이미지 유형별 정합성을 판단할 Magic Byte 정의
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "jpg",  new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "png",  new byte[]{(byte)0x89, 0x50, 0x4E, 0x47},
        "gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
        "webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    // 업로드를 허용할 확장자 화이트리스트
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * MultipartFile에 대해 확장자 화이트리스트 검사 및 Magic Byte 실제 내용 검증을 수행합니다.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new SecurityException("파일명이 누락되었습니다.");
        }

        String ext = StringUtils.getFilenameExtension(originalName);
        if (ext == null || ext.isBlank()) {
            throw new SecurityException("확장자를 판별할 수 없는 파일입니다.");
        }
        ext = ext.toLowerCase();

        // 1. 확장자 화이트리스트 검증
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("파일 업로드 거부: 허용되지 않는 확장자 - {}", ext);
            throw new SecurityException("허용되지 않는 파일 형식(확장자)입니다.");
        }

        // 2. Magic Byte 검증 (Content-Type 및 파일 시그니처 위장 우회 차단)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int readBytes = is.read(header, 0, 8);
            if (readBytes < 4) {
                throw new SecurityException("파일 크기가 유효성 검증을 수행하기에 너무 작습니다.");
            }

            // jpeg는 jpg와 동일한 매직 바이트 사용
            String targetExt = "jpeg".equalsIgnoreCase(ext) ? "jpg" : ext;
            byte[] magic = MAGIC_BYTES.get(targetExt);
            
            if (magic != null) {
                for (int i = 0; i < magic.length; i++) {
                    if (header[i] != magic[i]) {
                        log.warn("파일 업로드 거부: 파일 본문 시그니처 불일치. 확장자는 {} 이나 매직바이트 미스매치.", ext);
                        throw new SecurityException("업로드된 파일 내용이 실제 확장자 규격과 일치하지 않습니다.");
                    }
                }
            }
        } catch (IOException e) {
            log.error("파일 업로드 시그니처 분석 입출력 에러: {}", originalName, e);
            throw new SecurityException("파일 안전성 검증 중 입출력 오류가 발생했습니다.");
        }
    }
}
