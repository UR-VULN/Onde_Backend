package com.onde.core.validation;

import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public final class MultipartInputValidator {

    private MultipartInputValidator() {
    }

    public static void validateImageFiles(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() > ValidationLimits.MAX_IMAGES) {
            throw new ValidationException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        for (MultipartFile file : images) {
            validateOptionalImage(file);
        }
    }

    public static ImageUploadValidator.ValidatedImage validateOptionalImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > ValidationLimits.MAX_FILE_BYTES) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE, "첨부 파일 크기는 10MB를 초과할 수 없습니다.");
        }
        return ImageUploadValidator.validate(file);
    }
}
