package com.onde.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MockS3Uploader {

    private final AwsS3Service awsS3Service;

    public String upload(MultipartFile file, String dirName) {
        // 실제 및 모의 가동 분기를 모두 포함하는 AwsS3Service로 호출 위임
        return awsS3Service.upload(file, dirName);
    }
}
