package com.onde.core.validation;

import com.onde.core.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadValidatorTest {

    private static byte[] minimalJpeg() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
    }

    private static byte[] minimalPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }

    @Test
    @DisplayName("JPEG 확장자·MIME·매직 바이트가 일치하면 검증에 통과한다")
    void validate_jpeg_success() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", minimalJpeg());

        ImageUploadValidator.ValidatedImage validated = ImageUploadValidator.validate(file);

        assertThat(validated.extensionWithDot()).isEqualTo(".jpg");
        assertThat(validated.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("PNG 파일은 검증에 통과한다")
    void validate_png_success() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", minimalPng());

        ImageUploadValidator.ValidatedImage validated = ImageUploadValidator.validate(file);

        assertThat(validated.extensionWithDot()).isEqualTo(".png");
        assertThat(validated.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("악성 HTML 업로드는 거부된다")
    void validate_rejects_html() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "evil.html",
                "text/html",
                "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> ImageUploadValidator.validate(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("확장자");
    }

    @Test
    @DisplayName("JPG 확장자 위장 SVG/HTML 페이로드는 매직 바이트 불일치로 거부된다")
    void validate_rejects_disguised_script() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "xss.jpg",
                "image/jpeg",
                "<svg onload=alert(1)>".getBytes());

        assertThatThrownBy(() -> ImageUploadValidator.validate(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("이미지 형식");
    }

    @Test
    @DisplayName("경로 조작 파일명(../)은 거부된다")
    void validate_rejects_path_traversal_filename() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "../shell.jsp",
                "image/jpeg",
                minimalJpeg());

        assertThatThrownBy(() -> ImageUploadValidator.validate(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("파일명");
    }

    @Test
    @DisplayName("대소문자 혼합 JPG 확장자는 허용된다")
    void validate_allows_uppercase_extension() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "PHOTO.JPG", "image/jpeg", minimalJpeg());

        assertThat(ImageUploadValidator.validate(file).contentType()).isEqualTo("image/jpeg");
    }
}
