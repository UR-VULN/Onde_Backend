package com.onde.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MockS3Uploader {

    private final AwsS3Service awsS3Service;

    public String upload(MultipartFile file, String dirName) {
        // 1. 원본 파일명에서 확장자만 안전하게 추출 (경로 탐색 문자 원천 차단)
        String originalName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalName);
        
        if (extension == null || extension.isBlank()) {
            extension = "png"; // 확장자가 없는 악의적 파일일 경우 기본값 부여
        }
        
        // 2. 서버가 100% 통제하는 안전한 새 파일명 생성
        String safeFileName = UUID.randomUUID().toString() + "." + extension;

        // 3. 파일명을 강제로 변경하기 위해 원본 파일을 Wrapper 객체로 감쌈
        MultipartFile safeFile = new MultipartFileWrapper(file, safeFileName);

        // 안전하게 이름이 세탁된 파일을 AwsS3Service로 전달
        return awsS3Service.upload(safeFile, dirName);
    }

    /**
     * 원본 파일명을 안전한 파일명으로 바꿔치기 위한 내부 래퍼(Wrapper) 클래스
     */
    private static class MultipartFileWrapper implements MultipartFile {
        private final MultipartFile delegate;
        private final String safeFileName;

        public MultipartFileWrapper(MultipartFile delegate, String safeFileName) {
            this.delegate = delegate;
            this.safeFileName = safeFileName;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getOriginalFilename() {
            return safeFileName; // 취약한 원본 이름 대신 우리가 생성한 안전한 이름을 반환
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return delegate.getBytes();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            delegate.transferTo(dest);
        }
    }
}