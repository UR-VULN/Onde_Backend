# Onde 사용자/판매자 취약점 이행점검 및 코드 수정 가이드

| 항목 | 내용 |
|---|---|
| 기준 보고서 | Onde_사용자판매자_취약점초기진단보고서.docx (2026-06-23) |
| 기준 가이드라인 | SK Shieldus 2022 Web/API 개발보안 Guideline v3.0.0 (28항목) |
| 대상 시스템 | www.onde.click 사용자/판매자 화면·API (api-module) |
| 진단 방식 | Burp Suite Community Edition을 이용한 운영(Production) 환경 수동 진단 (샘플링) |
| 문서 목적 | 발견된 취약점에 대한 **코드 수정(시큐어 코딩) 이행점검 체크리스트** — 개발자가 바로 패치에 착수할 수 있도록 근본원인·Before/After 코드·재검증 방법을 정리 |
| 우선순위 산정 | 영향도(데이터 노출 범위/금전적 손실) × 악용 난이도 기준 정성 평가. CVSS 점수는 컨설턴트 추정치(참고용)이며 공식 스코어링 결과가 아님 |

> ⚠️ 본 문서는 "사용자/판매자 관점" 보고서의 28개 항목 + 기타취약점(8-1) 4건을 기준으로 작성되었습니다. 관리자(Admin) 관점 보고서와 연계되는 체인(예: 1-3 → 관리자 4-5 전체회원탈취)은 해당 항목에 "연계 영향"으로 표기했습니다.

---

## 1. 이행점검 진행 절차

```
① 코드 수정 (담당 개발자)
   ↓
② 코드 리뷰 (체크리스트 기준 자가/동료 검토)
   ↓
③ 스테이징 배포
   ↓
④ 재현 테스트 (Burp Repeater로 원본 PoC 요청 재전송 → 차단/무력화 확인)
   ↓
⑤ 본 문서 체크박스 갱신 + 스크린샷 증적 첨부
   ↓
⑥ 보고서 v2(이행점검 결과보고서)에 반영
```

각 항목의 `이행점검` 체크박스는 **① 코드수정 완료 / ② 재현테스트 통과 / ③ 회귀테스트 통과** 3단계로 구성됩니다.

---

## 2. 전체 현황 요약

| 우선순위 | 항목코드 | 취약점명 | CWE | 추정 심각도 | 상태 |
|---|---|---|---|---|---|
| **P0** | 1-4 | SSRF / LFI (정산서 PDF) — 시크릿·IAM 자격증명 탈취 확정 | CWE-918, CWE-22 | Critical (9.8) | 취약 |
| **P0** | 1-2 | UNION 기반 SQL Injection — 전체 회원 정보 탈취 | CWE-89 | Critical (9.1) | 취약 |
| **P0** | 1-3 | 파라미터 조작 권한상승(SUPER_ADMIN) | CWE-915 | Critical (9.0) | 취약 |
| **P1** | 1-6 | 결제 금액 변조 (항공권) | CWE-20, CWE-840 | High (8.1) | 취약 |
| **P1** | 2-1 | 악성코드 파일 업로드 검증 부재 | CWE-434 | High (7.5) | 취약 |
| **P1** | 5-2 | 개인정보(여권번호 등) 평문 노출 | CWE-200 | High (7.5) | 취약 |
| **P1** | 4-5 | IDOR — memberId 변조로 타인 명의 예약 생성 | CWE-639 | High (6.8) | 취약 |
| **P2** | 6-1 | 에러 메시지를 통한 내부 정보 노출 | CWE-209 | Medium (5.3) | 취약 |
| **P2** | 7-4 | 보안 헤더 누락 (CSP/HSTS) | CWE-693 | Medium (5.3) | 주의 |
| **P2** | 8-1 | 비즈니스 로직 결함 — 재고 선점(DoS성) | CWE-840 | Medium (5.3) | 취약 |
| **P2** | 3-2 | 로그인 무제한 브루트포스 | CWE-307 | Medium (5.3) | 취약 |
| **P2** | 7-3 | 서버 헤더(nginx 버전) 노출 | CWE-200 | Low (3.7) | 취약 |
| **P2** | 3-3 | User Enumeration (중복확인 API) | CWE-203 | Low (3.7) | 주의 |
| **P2** | 4-1 | 쿠키 SameSite=None | CWE-352 | Low (3.1) | 주의 |
| **P2** | 4-2 | JWT Payload 평문 노출 | CWE-200 | Low (2.7) | 주의 |
| **P2** | 7-1 | OPTIONS 응답 Allow 헤더 노출 | CWE-200 | Info (2.0) | 주의 |
| **P2** | 8-1 | 입력값 경계 검증 부재 (예약 인원) | CWE-20 | Low (3.1) | 주의 |
| **P3** | 8-1 | DoS (대용량 파일 업로드) — 테스트 필요 | CWE-400 | 미확정 | 미확정 |
| **P3** | 3-4 | 관리자 도메인명(admin) 추측 가능 | CWE-200 | Low | 주의 |
| **P3** | 3-5 | robots.txt 부재 | - | Info | 주의 |

조치 불필요(양호/N/A) 항목은 **8장**에 회귀 방지용으로 기록합니다.

---

## 3. P0 — Critical (보고서 작성보다 우선, 즉시조치)

### [1-4] SSRF / LFI — 정산서·리포트 PDF 생성 API

- **CWE**: CWE-918 (SSRF), CWE-22 (Path Traversal / LFI)
- **엔드포인트**: `POST /user-api/api/v1/report/integrated`
- **파라미터**: `logoUrl`, `template`
- **공격 재현**: `template:"../../../proc/self/environ"` → 200 OK + PDF 본문에 `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `KAKAO_CLIENT_SECRET`, `AES_SECRET_KEY`, `NTS_SERVICE_KEY` 등 전체 환경변수 노출. `logoUrl:"http://169.254.169.254/latest/meta-data/iam/security-credentials/onde-ec2-ssm-role"` → IAM 임시 자격증명(AccessKeyId 등) 노출.
- **근본 원인**: 클라이언트 입력값(`template`)이 검증 없이 파일시스템 경로 조합에 직접 사용되고, `logoUrl`이 검증 없이 `HttpURLConnection`/유사 객체로 그대로 연결됨.

**Before (취약 패턴)**
```java
// ReportController / ReportService
String filePath = TEMPLATE_BASE_PATH + request.getTemplate() + ".html";
File templateFile = new File(filePath); // template 값이 경로에 직접 결합됨

URL logo = new URL(request.getLogoUrl()); // 스킴/호스트 검증 없음
InputStream logoStream = logo.openConnection().getInputStream();
```

**After (수정안)**
```java
// 1) template: 화이트리스트 enum으로 전환 — 외부 입력이 경로에 절대 들어가지 않도록 함
public enum ReportTemplate {
    VERIFICATION("verification.html"),
    INTEGRATED("integrated.html"),
    SETTLEMENT("settlement.html");

    private final String fileName;
    ReportTemplate(String fileName) { this.fileName = fileName; }
    public String getFileName() { return fileName; }
}

@RequestParam ReportTemplate template  // 정의되지 않은 값은 Spring이 자동 400 처리
File templateFile = new File(TEMPLATE_BASE_PATH, template.getFileName());

// 2) logoUrl: 자사 도메인 화이트리스트 + 스킴 제한
private static final Set<String> ALLOWED_LOGO_HOSTS =
        Set.of("onde-assets.s3.ap-northeast-2.amazonaws.com", "cdn.onde.click");

URI uri = new URI(request.getLogoUrl());
if (!"https".equalsIgnoreCase(uri.getScheme())
        || !ALLOWED_LOGO_HOSTS.contains(uri.getHost())) {
    throw new InvalidLogoUrlException("허용되지 않은 logoUrl입니다.");
}
```

**보안 컨설턴트 권고 — 코드 수정과 별도로 반드시 병행할 조치**
1. **시크릿 즉시 교체(Rotate)**: 유출이 PDF로 직접 확인된 `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `KAKAO_CLIENT_SECRET`, `AES_SECRET_KEY`, `NTS_SERVICE_KEY` — 코드 배포 전이라도 우선 교체. 노출된 시점부터 교체 전까지는 침해된 것으로 간주.
2. **시크릿 보관 방식 전환**: 환경변수 평문 주입 → AWS Secrets Manager/Parameter Store(SecureString). 동일한 코드 결함이 재발해도 전체 시크릿이 한 번에 노출되지 않도록 피해 범위 축소.
3. **탈취된 IAM Role 자격증명 강제 회전**: 해당 Role의 신뢰 정책 재설정 또는 인스턴스 자격증명 강제 회전, IMDSv2(`http-tokens=required`) 강제 적용.
4. **IAM 최소권한 재검토**: 해당 Role에 SSM 등 과도한 권한이 부여되어 있는지 점검.

**이행점검**
- [ ] template 파라미터 enum 화이트리스트 전환 완료
- [ ] logoUrl 도메인/스킴 화이트리스트 적용 완료
- [ ] 노출 시크릿 6종 교체(rotate) 완료
- [ ] IMDSv2 강제 적용 확인
- [ ] Burp Repeater로 원본 PoC(`../../../proc/self/environ`, IMDS 경로) 재전송 → 차단 확인

---

### [1-2] UNION 기반 SQL Injection — 게시글 검색 API

- **CWE**: CWE-89
- **엔드포인트**: `GET /user-api/api/v1/posts?status=`
- **파라미터**: `status`
- **공격 재현**: `status=ACTIVE' UNION SELECT ... GROUP_CONCAT(email), GROUP_CONCAT(password) ... FROM members-- -` → 인증된 일반 USER 권한만으로 전체 회원 이메일+bcrypt 해시 탈취.
- **근본 원인**: `status` 파라미터가 PreparedStatement/파라미터 바인딩 없이 쿼리에 직접 결합됨. 동일 컨트롤러의 `keyword` 파라미터는 바인딩이 적용되어 안전했던 것과 대조적으로, **파라미터별로 방어 수준이 다른** 것이 핵심 문제.

**Before (취약 패턴)**
```java
@GetMapping("/posts")
public ResponseEntity<?> getPosts(@RequestParam String status) {
    return postRepository.findByStatusRaw(status); // 문자열 그대로 쿼리에 결합
}
```

**After (수정안)**
```java
// 1) status를 enum으로 바인딩 — 정의되지 않은 값은 컨트롤러 진입 전 400 처리됨
public enum PostStatus { ACTIVE, HIDDEN, DELETED }

@GetMapping("/posts")
public ResponseEntity<?> getPosts(@RequestParam PostStatus status) {
    return ResponseEntity.ok(postService.findByStatus(status));
}

// 2) Repository: JPA 파라미터 바인딩 (PreparedStatement 자동 적용)
@Query("SELECT p FROM Post p WHERE p.status = :status")
List<Post> findByStatus(@Param("status") PostStatus status);
```

**일괄 점검 권고**: 동일 코드베이스 내 다른 동적 쿼리 파라미터(관리자 검색 API `keyword` 등, 관리자 보고서 1-2 "미확정" 항목 포함)에 대해서도 PreparedStatement/JPA 바인딩 적용 여부를 전수 점검해야 합니다.

**이행점검**
- [ ] status 파라미터 enum 전환 및 JPA 파라미터 바인딩 적용
- [ ] 동일 컨트롤러/타 컨트롤러 동적 쿼리 파라미터 전수 점검 완료
- [ ] CloudWatch 등으로 UNION/비정상 응답 길이 탐지 알람 구성 (선택, 2차 방어선)
- [ ] Burp Repeater로 원본 UNION 페이로드 재전송 → 400/안전 처리 확인

---

### [1-3] 파라미터 값 조작을 통한 권한상승 (SUPER_ADMIN)

- **CWE**: CWE-915 (Mass Assignment)
- **엔드포인트**: `POST /user-api/api/v1/auth/signup`
- **파라미터**: `role`
- **공격 재현**: 회원가입 Body에 `"role":"SUPER_ADMIN"`을 강제 주입 → 201 Created로 SUPER_ADMIN 계정 생성. 해당 토큰으로 관리자 API(`GET /admin-api/api/v1/admin/members`) 접근 시 전체 회원 목록 탈취까지 연결됨 (**연계 영향**: 관리자 보고서 1-3/4-5).
- **근본 원인**: DTO의 `role` 필드를 서버가 클라이언트 입력값 그대로 신뢰함.

**Before (취약 패턴)**
```java
public class SignupRequest {
    private String email;
    private String password;
    private String role; // 클라이언트가 임의 값 주입 가능
}

member.setRole(Role.valueOf(request.getRole()));
```

**After (수정안)**
```java
// DTO에서 role 필드 자체를 제거 — 회원가입 시 권한은 클라이언트 입력으로 받지 않음
public class SignupRequest {
    private String email;
    private String password;
    // role 필드 삭제
}

// 서비스 레이어에서 서버가 강제 고정
Member member = Member.builder()
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER) // 항상 USER로 고정, 예외 없음
        .build();
```

**근본 원인 패턴 재발 방지**: 동일한 Mass Assignment 패턴이 관리자의 "회원→SELLER 역할전환 API"의 `roles` 필드에도 동일하게 존재합니다(관리자 보고서 4-5). DTO 단위로 "클라이언트가 보내도 서버가 무시해야 하는 필드"를 별도 리스트로 관리하고, 코드리뷰 체크리스트에 포함하시길 권고합니다.

**이행점검**
- [ ] SignupRequest DTO에서 role 필드 제거
- [ ] 서비스 레이어 role 강제 고정(USER) 적용
- [ ] 동일 패턴(Mass Assignment) 존재하는 타 API 전수 점검 (관리자 역할전환 API는 별도 트랙에서 조치)
- [ ] Burp Repeater로 `role:SUPER_ADMIN` 주입 PoC 재전송 → USER로 강제 처리 확인

---

## 4. P1 — High (이번 스프린트 내 조치)

### [1-6] 결제 금액 변조 — 항공권 예약

- **CWE**: CWE-20, CWE-840 (비즈니스 로직 결함)
- **엔드포인트**: `POST /user-api/api/v1/flights/booking`
- **파라미터**: `totalPrice`
- **현황**: 렌터카/숙소 객실 예약은 서버가 실제 가격을 재조회해 클라이언트 입력값을 덮어쓰지만, **항공권 예약 컨트롤러만** 클라이언트가 보낸 `totalPrice`를 그대로 신뢰함. `totalPrice:1`로 변조 시 1원에 비즈니스석 예약 성공.

**Before**
```java
Reservation reservation = Reservation.builder()
        .totalPrice(request.getTotalPrice()) // 클라이언트 입력 그대로 신뢰
        .build();
```

**After**
```java
FlightSchedule schedule = flightScheduleRepository.findById(request.getScheduleId())
        .orElseThrow(() -> new ScheduleNotFoundException());

BigDecimal actualPrice = priceCalculator.calculate(schedule, request.getSeatClass(), request.getPassengerCount());

Reservation reservation = Reservation.builder()
        .totalPrice(actualPrice) // 서버가 재계산한 값만 사용, 클라이언트 totalPrice는 완전히 무시
        .build();
```

**이행점검**
- [ ] 항공권 예약 로직에 렌터카/숙소와 동일한 서버 재계산 패턴 적용
- [ ] 결제 관련 전체 컨트롤러(항공권/렌터카/숙소/기타) 가격 검증 패턴 일괄 점검
- [ ] Burp Repeater로 `totalPrice:1` PoC 재전송 → 서버 재계산값으로 처리 확인

---

### [2-1] 악성코드 파일 업로드 검증 부재

- **CWE**: CWE-434
- **엔드포인트**: `POST /user-api/api/v1/posts`(images), `POST /seller-api/api/v1/accommodations`(thumbnail)
- **현황**: ① PNG 파일의 filename만 `hacker.php`로 변조 → Content-Type만 확인하고 실제 확장자/Magic Byte 미검증으로 `.php` 확장자 그대로 업로드 성공. ② SVG(`<script>` 포함) 썸네일 업로드 시 확장자·Content-Type 검증 없이 그대로 저장.

**Before**
```java
String originalFilename = file.getOriginalFilename();
String contentType = file.getContentType(); // Content-Type만 확인
s3Client.putObject(bucket, originalFilename, file.getInputStream(), ...); // 원본 파일명 그대로 사용
```

**After**
```java
private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
    "jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
    "png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}
    // ...
);

String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
if (!ALLOWED_EXT.contains(ext)) {
    throw new InvalidFileTypeException("허용되지 않은 파일 형식입니다.");
}
if (!matchesMagicByte(file.getInputStream(), ext)) {
    throw new InvalidFileTypeException("파일 내용이 확장자와 일치하지 않습니다.");
}

// 서버가 파일명을 재생성 — 원본 파일명/확장자를 신뢰하지 않음
String s3Key = UUID.randomUUID() + "." + ext;
s3Client.putObject(bucket, s3Key, file.getInputStream(), ...);
```

**SVG 처리 방침**: SVG는 업로드 허용 확장자 목록에서 제외(권고)하거나, 허용해야 한다면 서버에서 `<script>`, `<foreignObject>` 등 위험 태그를 제거 후 재인코딩(예: `OWASP Java HTML Sanitizer` 또는 SVG 전용 sanitizer 라이브러리 적용)해야 합니다.

**이행점검**
- [ ] 확장자 화이트리스트 + Magic Byte 검증 적용
- [ ] S3 업로드 시 서버 측 파일명 재생성(UUID) 적용
- [ ] SVG 업로드 제외 또는 sanitize 로직 적용
- [ ] Burp Repeater로 `.php` 확장자 위장 업로드 PoC 재전송 → 차단 확인

---

### [5-2] 개인정보 평문 노출 — 마이페이지 예약 조회

- **CWE**: CWE-200
- **엔드포인트**: `GET /user-api/api/v1/members/me/reservations/*`
- **현황**: `passengerName`, `bookingCode`, `totalPrice` 등이 마스킹 없이 평문 응답에 포함.

**Before**
```java
public class ReservationResponse {
    private String passengerName;
    private String bookingCode;
    // ...
}
```

**After**
```java
public class ReservationResponse {
    private String passengerName; // 마스킹 처리된 값만 노출
    private String bookingCode;

    public static ReservationResponse from(Reservation r) {
        return ReservationResponse.builder()
                .passengerName(MaskingUtils.maskName(r.getPassengerName())) // 홍**동
                .bookingCode(MaskingUtils.maskMiddle(r.getBookingCode()))   // M1******8
                .build();
    }
}
```

**이행점검**
- [ ] 응답 DTO에 마스킹 유틸 적용
- [ ] 클라이언트에 불필요한 필드 응답 데이터 최소화(Response DTO 슬리밍)
- [ ] Burp Repeater로 원본 응답 재요청 → 마스킹 처리 확인

---

### [4-5] IDOR — memberId 변조를 통한 타인 명의 예약 생성

- **CWE**: CWE-639 (Broken Object Level Authorization)
- **엔드포인트**: `POST /user-api/api/v1/rooms/booking` 등 예약 생성 전체, `DELETE /api/v1/reservations/{id}`
- **현황**: 본인(memberId 7) 토큰으로 인증된 상태에서 Body의 `memberId`만 8로 변조 → 타인 명의 예약이 그대로 생성됨. 서버가 JWT가 아닌 클라이언트가 보낸 Body의 memberId를 신뢰.

**Before**
```java
Reservation reservation = Reservation.builder()
        .memberId(request.getMemberId()) // 클라이언트 입력 신뢰
        .build();
```

**After**
```java
@PostMapping("/rooms/booking")
public ResponseEntity<?> book(@AuthenticationPrincipal CustomUserDetails principal,
                               @RequestBody RoomBookingRequest request) {
    Long memberId = principal.getMemberId(); // JWT에서 추출, Body의 memberId는 무시
    Reservation reservation = Reservation.builder()
            .memberId(memberId)
            .build();
    // ...
}

// 소유권 검증 — 조회/삭제 전체에 공통 적용
private void verifyOwnership(Reservation reservation, Long requesterMemberId) {
    if (!reservation.getMemberId().equals(requesterMemberId)) {
        throw new AccessDeniedException("본인 소유의 예약만 처리할 수 있습니다.");
    }
}
```

**추가 확인 필요(미확정 부분)**: `DELETE /api/v1/reservations/{id}`에서 본인 토큰으로 타인 소유 `reservationId` 삭제 시도가 실제로 차단되는지 재현 테스트가 필요합니다. `reservationId`가 순차 정수라 추측 난이도가 낮으므로 우선순위를 낮추지 마십시오.

**이행점검**
- [ ] 모든 예약 생성 API에서 memberId를 Body가 아닌 JWT principal에서 추출
- [ ] 모든 예약 조회/삭제 API에 소유권 검증 로직(`verifyOwnership`) 공통 적용
- [ ] DELETE 엔드포인트 IDOR 재현 테스트(타인 reservationId로 삭제 시도) 수행 및 결과 기록
- [ ] Burp Repeater로 memberId 변조 PoC 재전송 → 본인 memberId로 강제 처리 확인

---

## 5. P2 — Medium (2주 내 조치, 일반 항목)

### [3-2] 로그인 무제한 브루트포스

- **CWE**: CWE-307 | **엔드포인트**: `POST /user-api/api/v1/auth/login`
- **현황**: 연속 로그인 시도 시 계정 잠금/429/CAPTCHA 등 어떠한 방어도 없음.

```java
// Redis 기반 Rate Limiter 예시 (Bucket4j 또는 직접 구현)
@PostMapping("/auth/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String key = "login_attempt:" + request.getEmail();
    long attempts = redisTemplate.opsForValue().increment(key, 1);
    if (attempts == 1) redisTemplate.expire(key, Duration.ofMinutes(15));
    if (attempts > 5) {
        throw new TooManyAttemptsException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        // 429 Too Many Requests 반환
    }
    // ... 인증 성공 시 redisTemplate.delete(key);
}
```
- 권고: 5회 이상 실패 시 계정 임시 잠금(또는 Rate Limiting 429), CAPTCHA 도입 검토. **관리자 로그인(`/auth/admin/login`)에도 동일하게 적용 필요** (관리자 보고서 3-2 — 영향도가 더 높으므로 더 엄격한 기준(3회/CAPTCHA 필수) 권고).

**이행점검**
- [ ] 일반 로그인 Rate Limiting/계정잠금 적용
- [ ] 관리자 로그인에 더 엄격한 기준 적용
- [ ] Burp Intruder로 동일 PoC 재실행 → 차단 확인

---

### [3-3] User Enumeration — 회원가입 중복확인 API

- **CWE**: CWE-203 | **엔드포인트**: `GET /api/v1/auth/check-email`, `check-nickname`
- **현황**: 로그인은 안전(동일 메시지)하나, 중복확인 API는 가입여부에 따라 응답 메시지가 명확히 다름.

```java
// Before: "사용 가능한 이메일입니다." / "이미 사용중인 이메일입니다." (구분됨)
// After: 중립적 문구로 통일 + Rate Limiting 추가
return ResponseEntity.ok(ApiResponse.success("확인이 완료되었습니다.", Map.of("available", isAvailable)));
// → 메시지는 통일하되 무차별 조회 자체를 어렵게 하는 것이 핵심이므로 Rate Limiting을 우선 적용
```

**이행점검**
- [ ] 응답 메시지 중립화
- [ ] 해당 엔드포인트 Rate Limiting 적용
- [ ] Burp Intruder로 이메일/닉네임 무차별 조회 재현 → 제한 확인

---

### [6-1] 에러 메시지를 통한 정보노출 — GlobalExceptionHandler 설계 결함

- **CWE**: CWE-209 | **현황**: `NoResourceFoundException` 등 본래 4xx인 예외가 500으로 처리되며 `systemMessage`에 Spring 내부 클래스 패키지 경로가 노출됨. (admin-module 동일 결함 존재)

```java
// Before: 포괄적 catch-all이 모든 예외를 500 + 상세 클래스명으로 응답

// After: 예외 타입별 정확한 매핑
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ErrorResponse.of("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(ErrorResponse.of("METHOD_NOT_ALLOWED", "허용되지 않은 메서드입니다."));
    }

    @ExceptionHandler(Exception.class) // 진짜 예상 못한 예외만 500
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e); // 상세는 서버 로그에만 기록
        return ResponseEntity.status(500).body(ErrorResponse.of("INTERNAL_ERROR", "처리 중 오류가 발생했습니다."));
        // systemMessage 필드 자체를 프로덕션 응답에서 제거
    }
}
```
- `application.yml`: `server.error.include-stacktrace: never`, `server.error.include-message: never` (프로덕션 프로필)

**이행점검**
- [ ] 예외 타입별 4xx 매핑 재설계
- [ ] systemMessage(상세 클래스명) 프로덕션 응답에서 제거
- [ ] admin-module 동일 결함 동시 조치
- [ ] Burp Repeater로 원본 404/405 PoC 재전송 → 클래스 경로 미노출 확인

---

### [4-1] 쿠키 SameSite=None / [4-2] JWT Payload 평문

```java
// [4-1] 쿠키 SameSite 수정
ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict") // None → Strict (또는 OAuth 리다이렉트 흐름이 있다면 Lax)
        .path("/")
        .maxAge(Duration.ofMinutes(30))
        .build();

// [4-2] JWT Payload 최소화 — 이메일 대신 memberId만 클레임에 포함
Claims claims = Jwts.claims();
claims.put("memberId", member.getId()); // email/role 평문 노출 최소화
// role이 필요하다면 권한 검증은 서버가 DB/캐시에서 재조회하여 처리
```
> 참고: alg/role 변조 토큰은 서명 검증으로 이미 정상 거부됨(양호 확인됨). Refresh Token 무효화 로직도 정상 동작(안전 확정). 이번 조치는 **정보노출 최소화** 목적이며 인증 우회 위험 자체는 없음.

**이행점검**
- [ ] SameSite=Strict(또는 Lax) 적용
- [ ] JWT Payload에서 email 등 불필요 클레임 제거, memberId 기반으로 전환
- [ ] 재배포 후 로그인 플로우 정상 동작 회귀테스트

---

### [7-1]/[7-3]/[7-4] 서버/보안 헤더 관련 (Allow 노출 / nginx 버전 노출 / CSP·HSTS 누락)

```nginx
# nginx.conf
server_tokens off;  # [7-3] Server: nginx/x.x.x 버전 제거

add_header Content-Security-Policy "default-src 'self'; script-src 'self'; object-src 'none';" always;  # [7-4]
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;  # [7-4]
# X-Frame-Options, X-Content-Type-Options는 이미 설정되어 있음(양호) — 유지
```
```java
// [7-1] OPTIONS 응답의 Allow 헤더에 메서드 목록이 그대로 노출되는 문제
// Spring WebMvcConfigurer에서 OPTIONS 자동 처리 비활성화 또는 CORS 설정에서 allowedMethods를 명시적으로 제한
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedMethods("GET", "POST") // 실제 사용하는 메서드만 명시, Allow 헤더 응답 범위 최소화
            .allowedOrigins("https://www.onde.click");
}
```

**이행점검**
- [ ] nginx `server_tokens off` 적용
- [ ] CSP/HSTS 헤더 추가 (X-XSS-Protection은 CSP로 대체되므로 제거 가능)
- [ ] OPTIONS Allow 헤더 노출 범위 최소화
- [ ] 1-1(XSS) 조치와 CSP를 함께 검증 — CSP가 인라인 스크립트를 차단하는지 회귀테스트

---

### [8-1] 비즈니스 로직 결함 — 재고 선점 (DoS성)

- **CWE**: CWE-840 | **현황**: 렌터카 예약 시 결제 미완료(RESERVED) 상태로도 재고가 선차감되어, 결제 의사 없이 반복 선점하면 실제 구매자의 예약을 차단 가능.

```java
// 결제 미완료 예약에 TTL 적용 — 스케줄러 또는 Redis TTL 락으로 자동 해제
@Scheduled(fixedRate = 60_000)
public void releaseExpiredHolds() {
    List<Reservation> expired = reservationRepository
            .findByStatusAndCreatedAtBefore(ReservationStatus.RESERVED, LocalDateTime.now().minusMinutes(10));
    expired.forEach(r -> {
        r.setStatus(ReservationStatus.CANCELLED);
        inventoryService.release(r.getCarId(), r.getStartDate(), r.getEndDate()); // 재고 복원
    });
}
```

**이행점검**
- [ ] 결제 미완료 예약(soft-hold)에 TTL(예: 10분) 적용
- [ ] TTL 만료 시 재고 자동 복원 로직 검증
- [ ] 반복 선점 시나리오 재현 테스트 → 일정 시간 후 재고 복원 확인

---

### [8-1] 입력값 경계 검증 부재 (예약 인원)

```java
public class RoomBookingRequest {
    @Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
    private Integer guests;
}
// 컨트롤러에 @Valid 적용 필수
@PostMapping("/rooms/booking")
public ResponseEntity<?> book(@Valid @RequestBody RoomBookingRequest request) { ... }
```

**이행점검**
- [ ] `@Min(1)` 등 Bean Validation 적용 및 `@Valid` 누락 여부 전수 점검
- [ ] guests:-100, 0 PoC 재전송 → 400 확인

---

## 6. P3 — Low / 추가 확인 필요

### [8-1] DoS (대용량 파일 업로드) — 테스트 미실행 상태

현재 "미확정"이므로 **선조치보다 먼저 재현 테스트 수행**이 필요합니다.

```yaml
# application.yml — 우선 적용 가능한 선제적 방어
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```
```nginx
client_max_body_size 10m;  # nginx 단에서도 동일하게 제한
```

**이행점검**
- [ ] 대용량 더미 파일 업로드 재현 테스트 실행 (응답시간/서버 자원 변화 관찰)
- [ ] 파일 크기 제한 설정값 적용
- [ ] 재테스트로 제한 동작 확인

### [3-4] 관리자 도메인명 추측 가능 / [3-5] robots.txt 부재

```
robots.txt 신규 생성:
User-agent: *
Disallow: /api/
Disallow: /admin/
```
- 관리자 도메인명(`admin.onde.click`)은 인프라/배포 설정 변경 사항이므로 별도 트랙(인프라팀)에서 비정형 명칭 검토 권고. 코드 레벨 조치는 robots.txt 추가가 우선.

**이행점검**
- [ ] robots.txt 생성 및 민감 경로 Disallow 적용
- [ ] 관리자 도메인명 변경 검토 사항을 인프라팀에 별도 전달

---

## 7. 조치 불필요 (양호/N/A) — 회귀 방지 기록

아래 항목은 현재 정상 동작이 확인되었으므로 **코드 수정은 불필요**하나, 향후 리팩토링 시 회귀(regression)가 발생하지 않도록 회귀테스트 항목에 포함해야 합니다.

| 항목 | 확인된 안전 동작 |
|---|---|
| 1-5 | OAuth2 redirect_uri/state 서버 측 검증 정상 |
| 3-1 | 비밀번호 정책(8~20자, 영문+숫자) 정상 적용 |
| 3-6 | 백업/설정 파일 경로 미존재 (Nginx try_files) |
| 4-3 | 메인 도메인 Actuator 비노출 (ALB 라우팅 미설정) |
| 4-4 | 비인증 상태 보호 API 접근 401 정상 차단 |
| 5-1 | Actuator env 미노출 |
| 6-2 | 에러 응답 JSON 포맷 일관성 확보 |
| 7-2 | 디렉토리 목록화 미노출 (S3/CloudFront 구조, autoindex off) |
| 8-1 | CORS — 비허용 Origin 정상 차단 |
| (렌터카/숙소) | 결제 금액 변조 시 서버 재계산 정상 동작 (1-6과 대조군) |
| (alg/role 변조 토큰) | JWT 서명 검증 정상, 위조 토큰 401 거부 (4-2) |
| (Refresh Token) | 재로그인 시 구 토큰 정상 무효화 (4-2) |

---

## 8. 부록 — 즉시 교체(Rotate) 대상 시크릿 목록

1-4(SSRF/LFI)로 노출이 **PDF 응답을 통해 직접 확인**된 시크릿입니다. 코드 수정 완료 여부와 무관하게 아래 목록은 **최우선으로 교체**해야 합니다.

- [ ] `DB_PASSWORD`
- [ ] `JWT_SECRET`
- [ ] `GOOGLE_CLIENT_SECRET`
- [ ] `KAKAO_CLIENT_SECRET`
- [ ] `AES_SECRET_KEY`
- [ ] `NTS_SERVICE_KEY`
- [ ] EC2 IAM Role(`onde-ec2-ssm-role`) 임시 자격증명 강제 회전

---

## 9. 서명/완료 확인

| 구분 | 담당자 | 완료일 | 비고 |
|---|---|---|---|
| 코드 수정 | | | |
| 코드 리뷰 | | | |
| 재현 테스트(PoC 재실행) | | | |
| 회귀 테스트 | | | |
| 시크릿 교체 확인 | | | |
| 보고서 v2 갱신 | | | |
