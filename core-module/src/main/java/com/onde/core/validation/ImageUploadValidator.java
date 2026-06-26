package com.onde.core.validation;

import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 업로드 이미지 확장자·MIME·매직 바이트·파일명 화이트리스트 검증 (2-1 악성 파일 업로드 방어).
 */
public final class ImageUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Pattern UNSAFE_FILENAME = Pattern.compile(".*(\\.\\.|/|\\\\|[\\u0000;]).*");

    private ImageUploadValidator() {
    }

    public record ValidatedImage(String extensionWithDot, String contentType) {
    }

    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "업로드 파일이 비어 있습니다.");
        }

        validateFilename(file.getOriginalFilename());
        validateDeclaredContentType(file.getContentType());

        byte[] header;
        try {
            header = readHeader(file, 16);
        } catch (IOException e) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "업로드 파일을 읽을 수 없습니다.");
        }

        DetectedFormat detected = detectFormat(header);
        if (detected == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않은 이미지 형식입니다. JPG, PNG, WEBP만 업로드할 수 있습니다.");
        }

        validateExtensionMatchesFormat(file.getOriginalFilename(), detected);
        return new ValidatedImage(detected.extensionWithDot(), detected.contentType());
    }

    private static void validateFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일명이 올바르지 않습니다.");
        }
        if (UNSAFE_FILENAME.matcher(originalFilename).matches()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일명에 허용되지 않은 문자가 포함되어 있습니다.");
        }
        String extension = extractExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않은 파일 확장자입니다. JPG, PNG, WEBP만 업로드할 수 있습니다.");
        }
    }

    private static void validateDeclaredContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일 Content-Type이 올바르지 않습니다.");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않은 파일 형식입니다. JPG, PNG, WEBP만 업로드할 수 있습니다.");
        }
    }

    private static void validateExtensionMatchesFormat(String originalFilename, DetectedFormat detected) {
        String extension = extractExtension(originalFilename);
        if (extension == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않은 파일 확장자입니다.");
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            if (!"image/jpeg".equals(detected.contentType())) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일 확장자와 실제 내용이 일치하지 않습니다.");
            }
            return;
        }
        if ("png".equals(extension) && !"image/png".equals(detected.contentType())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일 확장자와 실제 내용이 일치하지 않습니다.");
        }
        if ("webp".equals(extension) && !"image/webp".equals(detected.contentType())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "파일 확장자와 실제 내용이 일치하지 않습니다.");
        }
    }

    private static String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file, int length) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[length];
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            if (read == buffer.length) {
                return buffer;
            }
            byte[] actual = new byte[read];
            System.arraycopy(buffer, 0, actual, 0, read);
            return actual;
        }
    }

    private static DetectedFormat detectFormat(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return new DetectedFormat(".jpg", "image/jpeg");
        }
        if (header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return new DetectedFormat(".png", "image/png");
        }
        if (header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return new DetectedFormat(".webp", "image/webp");
        }
        return null;
    }

    private record DetectedFormat(String extensionWithDot, String contentType) {
    }
}
