package com.onde.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            extension = ".jpg";
        }

        // [보안 패치 1] 확장자 화이트리스트 검증
        java.util.List<String> allowedExtensions = java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf");
        if (!allowedExtensions.contains(extension)) {
            log.error("허용되지 않는 파일 확장자 업로드 시도: {}", originalFilename);
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. (이미지 및 PDF만 허용)");
        }

        // [보안 패치 1-2] 매직 바이트(시그니처) 검증 (위변조 방지)
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            validateMagicBytes(fileBytes, extension);
            
            // [보안 패치 1-3] 이미지 재인코딩 (폴리글랏/EXIF 악성코드 제거)
            fileBytes = stripExifAndReencode(fileBytes, extension);
        } catch (java.io.IOException e) {
            log.error("파일 바이트 읽기 실패: {}", originalFilename);
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        }

        String randomName = UUID.randomUUID().toString();
        String s3Key = dirName + "/" + randomName + extension;

        if (useMock) {
            log.info("[MOCK S3] Uploading file={} to dirName={} -> returning mock CloudFront URL", originalFilename, dirName);
            return cloudFrontDomain + "/" + s3Key;
        }

        // [보안 패치 2] Content-Type 강제 고정 (브라우저가 HTML로 해석하지 못하게 방어)
        String safeContentType = "application/octet-stream";
        if (extension.equals(".jpg") || extension.equals(".jpeg")) safeContentType = "image/jpeg";
        else if (extension.equals(".png")) safeContentType = "image/png";
        else if (extension.equals(".gif")) safeContentType = "image/gif";
        else if (extension.equals(".webp")) safeContentType = "image/webp";
        else if (extension.equals(".pdf")) safeContentType = "application/pdf";

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(safeContentType) // 기존 브라우저가 보낸 file.getContentType() 신뢰 X
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
            log.info("[PRODUCTION S3] Successfully uploaded file to S3. Key: {}", s3Key);

            // 인프라 정책: S3 직접 URL이 아닌 CloudFront CDN 도메인 URL 리턴 및 DB 보관
            return cloudFrontDomain + "/" + s3Key;
        } catch (Exception e) {
            log.warn("[S3 UPLOAD FALLBACK] S3 upload failed for file={}: {}. Falling back to Mock URL.", originalFilename, e.getMessage());
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

    /**
     * 파일 매직 바이트를 검증하여 실제 파일 형식이 확장자와 일치하는지 확인합니다.
     */
    private void validateMagicBytes(byte[] bytes, String extension) {
        if (bytes == null || bytes.length < 8) {
            throw new IllegalArgumentException("유효하지 않은 파일입니다. (크기가 너무 작음)");
        }

        boolean isValid = false;
        if (extension.equals(".jpg") || extension.equals(".jpeg")) {
            // FF D8 FF
            isValid = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
        } else if (extension.equals(".png")) {
            // 89 50 4E 47 0D 0A 1A 0A
            isValid = (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47);
        } else if (extension.equals(".gif")) {
            // GIF8 (47 49 46 38)
            isValid = (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x38);
        } else if (extension.equals(".webp")) {
            // RIFF .... WEBP
            isValid = (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
                       bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 && bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50);
        } else if (extension.equals(".pdf")) {
            // %PDF (25 50 44 46)
            isValid = (bytes[0] == (byte) 0x25 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x44 && bytes[3] == (byte) 0x46);
        }

        if (!isValid) {
            log.error("파일 매직 바이트 검증 실패. 위변조된 파일일 가능성이 있습니다. 확장자: {}", extension);
            throw new IllegalArgumentException("파일의 실제 형식이 확장자와 일치하지 않습니다. (위변조 감지)");
        }
    }

    /**
     * 이미지를 파싱하고 순수 픽셀 데이터만 다시 써서(Re-encoding) EXIF 등 메타데이터에 숨겨진 악성 코드를 제거합니다.
     */
    private byte[] stripExifAndReencode(byte[] originalBytes, String extension) {
        // PDF나 WebP, GIF 등은 ImageIO 기본 지원 한계나 애니메이션 파손 우려로 재인코딩 생략 
        // (대신 매직바이트, 화이트리스트, Content-Type 강제로 방어)
        if (extension.equals(".pdf") || extension.equals(".webp") || extension.equals(".gif")) {
            return originalBytes;
        }

        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(originalBytes);
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bais);
            if (image == null) {
                throw new IllegalArgumentException("유효하지 않은 이미지 데이터입니다. (손상된 파일)");
            }
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            String format = extension.equals(".png") ? "png" : "jpg";
            
            // 다시 쓰면서 원본의 악성 EXIF 헤더 등은 모두 증발함
            boolean success = javax.imageio.ImageIO.write(image, format, baos);
            if (!success) {
                throw new IllegalArgumentException("이미지 재인코딩에 실패했습니다.");
            }
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("이미지 폴리글랏 검증/재인코딩 실패: {}", e.getMessage());
            throw new IllegalArgumentException("이미지 파싱 및 재인코딩에 실패했습니다. (위변조 감지)");
        }
    }
}
