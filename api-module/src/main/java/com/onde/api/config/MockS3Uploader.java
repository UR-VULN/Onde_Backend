package com.onde.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MockS3Uploader {

    private final AwsS3Service awsS3Service;

    public String upload(MultipartFile file, String dirName) {
        // 운영(S3)과 로컬(MinIO/Mock) 분기를 모두 담고 있는 AwsS3Service로 위임
        return awsS3Service.upload(file, dirName);
    }
}
