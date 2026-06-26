package com.onde.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.onde.core.validation.ImageUploadValidator;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class AwsS3Service {

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.cloudfront-domain}")
    private String cloudFrontDomain;

    @Value("${aws.s3.region}")
    private String regionStr;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private boolean useMock = false;

    @PostConstruct
    public void init() {
        try {
            // AWS Credentials가 환경 변수나 프로필 등에 존재할 때만 정상 초기화 시도
            DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
            // 자격 증명 실존 여부 즉석 점검 (예외 발생 시 catch 블록으로 이동하여 Mock 가동)
            credentialsProvider.resolveCredentials();

            this.s3Client = S3Client.builder()
                    .region(Region.of(regionStr))
                    .credentialsProvider(credentialsProvider)
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(regionStr))
                    .credentialsProvider(credentialsProvider)
                    .build();

            log.info("AWS S3Client and S3Presigner initialized successfully in PRODUCTION mode.");
        } catch (Exception e) {
            log.warn("AWS Credentials not resolved: {}. Initializing S3Client for Local MinIO at {}...", e.getMessage(), s3Endpoint);
            try {
                this.s3Client = S3Client.builder()
                        .region(Region.of(regionStr))
                        .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("onde-s3-user", "onde-s3-password")))
                        .endpointOverride(java.net.URI.create(s3Endpoint))
                        .forcePathStyle(true) // MinIO 사용 시 필수 설정
                        .build();

                this.s3Presigner = S3Presigner.builder()
                        .region(Region.of(regionStr))
                        .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("onde-s3-user", "onde-s3-password")))
                        .endpointOverride(java.net.URI.create(s3Endpoint))
                        .build();

                this.useMock = false; // Mock 대신 실제 Local MinIO로 쓰기 로직 활성화
                log.info("S3Client and S3Presigner initialized successfully for Local MinIO.");
            } catch (Exception ex) {
                this.useMock = true;
                log.error("Failed to initialize Local MinIO S3Client. AWS S3 will run in MOCK Fallback mode.", ex);
            }
        }
    }

    /**
     * S3 버킷에 파일을 업로드하고, S3 직접 주소가 아닌 인프라 정책에 명시된 CloudFront CDN 도메인 URL로 반환합니다.
     */
    public String upload(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        ImageUploadValidator.ValidatedImage validated = ImageUploadValidator.validate(file);
        String randomName = UUID.randomUUID().toString();
        String s3Key = dirName + "/" + randomName + validated.extensionWithDot();

        if (useMock) {
            log.info("[MOCK S3] Uploading validated image to dirName={} -> returning mock CloudFront URL", dirName);
            return cloudFrontDomain + "/" + s3Key;
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(validated.contentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("[PRODUCTION S3] Successfully uploaded file to S3. Key: {}", s3Key);

            return cloudFrontDomain + "/" + s3Key;
        } catch (Exception e) {
            log.warn("[S3 UPLOAD FALLBACK] S3 upload failed for key={}: {}. Falling back to Mock URL.", s3Key, e.getMessage());
            return cloudFrontDomain + "/" + s3Key;
        }
    }

    /**
     * 민감한 파일(영수증, E-ticket) 다운로드를 위한 5분 유효 임시 Presigned URL을 GET 방식으로 발급합니다.
     */
    public String generatePresignedUrl(String dirName, String fileName) {
        String s3Key = dirName + "/" + fileName;

        if (useMock) {
            String mockUrl = cloudFrontDomain + "/private/" + s3Key + "?signature=mock_sig_" + UUID.randomUUID().toString().substring(0, 8) + "&expires=" + (System.currentTimeMillis() / 1000 + 300);
            log.info("[MOCK S3] Generating mock Presigned URL for private key={} -> {}", s3Key, mockUrl);
            return mockUrl;
        }

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5)) // 인프라 사양: 만료 시간 5분
                    .getObjectRequest(builder -> builder.bucket(bucket).key(s3Key).build())
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            log.info("[PRODUCTION S3] Generated 5-minute Presigned URL successfully for key={}", s3Key);
            return presignedUrl;
        } catch (Exception e) {
            log.error("[PRODUCTION S3] Presigned URL generation failed for key={}, error: {}", s3Key, e.getMessage());
            throw new RuntimeException("S3 Presigned URL 생성 중 오류가 발생했습니다.", e);
        }
    }
}
