# Onde WEB/API 취약점 조치 결과 보고서

본 문서는 Onde 웹/API 서비스의 보안 취약점 진단 결과를 바탕으로 수행된 시큐어 코딩 조치 및 코드 수정 내역을 기술합니다.

---

## 1. 입/출력 값 검증 부재 및 주입 방지

### [1-1] Stored XSS 취약점 조치
* **취약점 원인:** 게시글 작성 및 수정 시 입력값에 대한 서버측 필터링이나 이스케이프 처리가 부재하여 악성 스크립트가 DB에 그대로 저장되는 위험이 존재했습니다.
* **수정된 코드:** [PostService.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/community/PostService.java)
  * **수정 내용:** 게시글의 제목(`title`)과 내용(`content`)을 DB에 저장하기 전 스프링의 `HtmlUtils.htmlEscape()`를 사용해 특수 스크립트 문자 태그를 이스케이프화(HTML Entity 변환) 조치했습니다.
  ```java
  // 수정 후 (Post 생성 및 수정 시 공통 적용)
  post.setTitle(org.springframework.web.util.HtmlUtils.htmlEscape(req.getTitle()));
  post.setContent(org.springframework.web.util.HtmlUtils.htmlEscape(req.getContent()));
  ```

### [1-2] UNION 기반 SQL Injection 취약점 조치
* **취약점 원인:** 커스텀 포스트 레포지토리에서 네이티브 쿼리를 직접 작성할 때 입력값(`status`, `type`)을 검증 없이 문자열 더하기 방식으로 싱글 쿼테이션(`'`) 사이에 대입하여 SQL Injection 공격 경로가 노출되어 있었습니다.
* **수정된 코드:** [PostRepositoryCustomImpl.java](file:///c:/Onde/Onde_backend/Onde_Backend/core-module/src/main/java/com/onde/core/repository/PostRepositoryCustomImpl.java) 및 [PostController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/community/PostController.java)
  * **수정 내용:** 
    1. 레포지토리 네이티브 쿼리를 파라미터 바인딩 방식(`:status`, `:type`) 및 `.setParameter()` 사용 방식으로 교체했습니다.
    2. 컨트롤러에서 검색 쿼리 상태 파라미터를 일반 `String` 타입이 아닌 `PostStatus` Enum 타입으로 강제 수신하여 올바르지 않은 상태 값이 유입되면 스프링 단에서 400 에러를 반환하게 검증을 보강했습니다.
  ```java
  // PostRepositoryCustomImpl.java 수정 후
  String sql = "SELECT * FROM posts WHERE status = :status";
  return em.createNativeQuery(sql, Post.class)
            .setParameter("status", status)
            .getResultList();
  ```

### [1-3] 최고 관리자(SUPER_ADMIN) 우회 가입 조작 취약점 조치
* **취약점 원인:** 회원가입 시 요청 Body에 클라이언트가 임의의 `role` 값을 주입할 때 서버 측에서 검증하지 않고 최고 관리자(`SUPER_ADMIN`) 권한으로 바로 승격 및 가입 처리가 진행되었습니다.
* **수정된 코드:** [AuthService.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/auth/AuthService.java)
  * **수정 내용:** 요청된 권한(`role`)이 허용 범주인 `USER` 또는 `SELLER` 권한 이외인 경우(예: `SUPER_ADMIN` 등 권한 변조 시도), 임의 기본값으로 넘기는 것이 아니라 즉시 `IllegalArgumentException("올바르지 않은 가입 권한입니다.")` 예외를 던져 가입을 거부하도록 조치했습니다.
  ```java
  // 수정 후
  MemberRole role = request.getRole() != null ? request.getRole() : MemberRole.USER;
  if (role != MemberRole.USER && role != MemberRole.SELLER) {
      throw new IllegalArgumentException("올바르지 않은 가입 권한입니다.");
  }
  ```

### [1-4] 정산서 출력 기능의 LFI 및 SSRF 취약점 조치
* **취약점 원인:** 정산서 PDF 출력 기능 호출 시 넘어오는 `template` 및 `logoUrl`을 경로 및 서버 검증 없이 사용하여, 로컬 시스템 환경변수 정보 유출(LFI) 및 AWS IMDS 토큰 탈취(SSRF) 공격 경로로 악용되는 백도어 성격의 테스트 샌드박스 로직이 포함되어 있었습니다.
* **수정된 코드:** [IntegratedReportController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/accommodation/IntegratedReportController.java)
  * **수정 내용:** 
    1. 컨트롤러 시작점에 파라미터 유효성 검증을 도입하여 `template`은 화이트리스트(`verification`, `business`) 값만, `logoUrl`은 오직 고정 경로(`https://onde.click/assets/logo.png`)만 수용되도록 강제했습니다.
    2. 로컬 파일을 무작위로 읽거나(`File` / `Files`) 임의의 URL에 아웃바운드 HTTP 요청을 행하던(`RestTemplate`) 진단 전용 샌드박스 코드 블록을 완전히 삭제했습니다.

### [1-6] 항공권 결제 금액 위변조 취약점 조치
* **취약점 원인:** 항공권 예약 요청 API 호출 시 클라이언트가 body에 담아 보내온 임의의 `totalPrice` 가격 정보를 그대로 수용하여 단돈 1원으로 비즈니스석 예약을 탈취할 수 있는 취약점이 존재했습니다.
* **수정된 코드:** [FlightService.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/flight/FlightService.java)
  * **수정 내용:** 클라이언트에서 전송된 결제 금액 파라미터를 사용하지 않고, 항공 스케줄 DB 내 실제 등록된 좌석 기본 단가(`inventory.getBasePrice()`)를 가져와 탑승 인원 수만큼 곱해 서버 내부에서 금액을 직접 자동 재산출하여 바인딩 처리하도록 차단 조치했습니다.
  ```java
  // 수정 후
  .totalPrice(inventory.getBasePrice().multiply(java.math.BigDecimal.valueOf(passengerCount)))
  ```

---

## 2. 쿠키 보안, 인증 토큰 및 접근 제어 개선

### [3-3] 회원가입 중복확인 시 계정 조사(User Enumeration) 취약점 조치
* **취약점 원인:** 이메일 및 닉네임 중복 체크 API 응답 시 가입 완료 상태와 사용 가능 상태에 각각 다른 메시지를 리턴하여, 악의적인 사용자가 사이트 내 회원 데이터베이스 존재 여부를 수집할 수 있었습니다.
* **수정된 코드:** [AuthController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/auth/AuthController.java)
  * **수정 내용:** 결과값이 중복 여부에 상관없이 동일하게 중립적인 안내 응답 메시지(`"이메일/닉네임 중복 확인이 완료되었습니다."`)를 전송하도록 일관되게 규격화했습니다.

### [4-1] JWT 쿠키의 SameSite 설정 누락 조치 (CSRF 방어)
* **취약점 원인:** 발급되는 JWT 인증 쿠키의 SameSite 속성이 `None`으로 설정되어 있어, 타사 도메인을 통한 강제적 크로스 사이트 인증 요청 유도(CSRF)에 노출되어 있었습니다.
* **수정된 코드:** [AuthController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/auth/AuthController.java) 및 [OAuth2AuthenticationSuccessHandler.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/security/oauth2/OAuth2AuthenticationSuccessHandler.java)
  * **수정 내용:** Access/Refresh 쿠키의 `sameSite` 설정을 기존 `None`에서 외부 도메인 전송 차단 형태인 `Lax` 정책으로 일괄 승격 시켰습니다.

### [4-2] JWT Payload 이메일 정보 노출 취약점 조치
* **취약점 원인:** JWT 토큰 Base64 디코딩 시 유저의 이메일 및 권한 속성이 평문 정보 상태로 그대로 노출되어 개인 식별이 가능한 수준의 데이터가 암호화되지 않고 있었습니다.
* **수정된 코드:** `AuthService.java`, `EmailAuthService.java`, `OAuth2AuthenticationSuccessHandler.java`, `CustomUserDetailsService.java`
  * **수정 내용:** JWT 토큰 Subject에 기재되던 민감 정보인 이메일을 삭제하고, 의미를 유추할 수 없는 유저 고유 PK 정보인 `memberId.toString()`을 Subject 값으로 변경하여 암호화/발급하도록 개선했습니다. 스프링 시큐리티의 `loadUserByUsername` 인증 필터 역시 이를 고려하여 회원 ID(Long) 조회를 기본 지원하고 예외 시 기존 폴백을 진행하도록 연동 코드를 개선했습니다.

### [4-5] 수평적 권한 상승(BOLA/IDOR) 취약점 조치
* **취약점 원인:** 객실 및 차량 예약 요청 시 본인 인증 토큰 정보와 상관없이 body에 실려오는 `memberId`를 우선 처리하여 타인의 계정 명의로 대리 예약 및 결제 청구를 야기할 수 있었습니다.
* **수정된 코드:** [AccommodationController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/accommodation/AccommodationController.java)
  * **수정 내용:** 요청 본문에 실려온 `memberId`를 신뢰하지 않고 무조건 덮어쓰기하여, 스프링 시큐리티 인증 토큰 파싱 필터(`@LoginMember userId`)에서 서버가 검증/추출한 실 로그인 유저 ID 값을 예약 생성 파라미터로 할당되도록 강제화했습니다.
  ```java
  // 수정 후 (reserveRoom 및 reserveCar 공통)
  req.setMemberId(userId); // 클라이언트 전달값을 시큐리티 컨텍스트 ID로 무조건 덮어씀
  ```

---

## 3. 부적절한 오류 처리 및 인프라 보안 개선

### [6-1] 오류 응답 상세 스택 트레이스 정보 노출 조치
* **취약점 원인:** 예측하지 못한 500 예외가 발생할 때 스프링 `GlobalExceptionHandler`에 구현된 예외 로직의 결과 객체가 에러 클래스 종류, 자바 패키지 정보, 오류 파싱 스택 내역(`systemMessage`)을 그대로 브라우저로 덤프하여 반환하고 있었습니다.
* **수정된 코드:** [GlobalExceptionHandler.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/exception/GlobalExceptionHandler.java)
  * **수정 내용:** 500 Internal Server Error 핸들러 응답의 `systemMessage` 값을 `"Internal Server Error"`라는 규격화된 메시지로 감추어 외부에 상세 동작 원리와 모듈 및 코드 구조가 유출되지 않도록 전면 마스킹 처리했습니다. 또한, `NoResourceFoundException` (404) 및 `HttpRequestMethodNotSupportedException` (405)에 대한 구체적 예외 매핑을 추가하여 프레임워크 스택 정보 유출 위험을 차단하였습니다.

### [2-1] 악성코드 파일 업로드 검증 부재 조치
* **취약점 원인:** 이미지 업로드 시 Content-Type 헤더 정보만 확인하고 파일 확장자 및 매직 바이트를 온전히 검사하지 않아, 우회 시도를 통해 악성 파일이 스토리지(S3 등)에 업로드되어 잠재적인 웹쉘 실행 및 Stored XSS 위협을 가할 수 있었습니다.
* **수정된 코드:** [AwsS3Service.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/config/AwsS3Service.java)
  * **수정 내용:** 업로드 허용 확장자 화이트리스트(`jpg, jpeg, png, gif`)를 지정하여 허용되지 않은 파일 형식은 업로드를 거부하고, 파일 내부 바이너리 헤더 시그니처를 검증하는 Magic Byte 판별 로직(`matchesMagicByte`)을 이식하여 확장자 조작 우회 공격을 차단했습니다.
  ```java
  // 수정 후 - Magic Byte 및 확장자 검증 로직 추가
  private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
  private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
      "jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
      "png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}
      // ...
  );

  String extName = extension.replace(".", "").toLowerCase();
  if (!ALLOWED_EXT.contains(extName)) {
      throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
  }
  if (!matchesMagicByte(file.getInputStream(), extName)) {
      throw new IllegalArgumentException("파일 내용이 확장자와 일치하지 않습니다.");
  }
  ```

### [5-2] 개인정보 평문 노출 조치
* **취약점 원인:** 마이페이지 예약 조회 API 호출 시 응답 결과 데이터 필드에 승객의 실제 전체 이름과 예약 코드 정보가 마스킹 없이 평문 상태로 전달되고 있었습니다.
* **수정된 코드:** [MaskingUtils.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/utils/MaskingUtils.java) (신규), [MemberMyPageService.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/member/MemberMyPageService.java)
  * **수정 내용:** 이름(`홍*동`) 및 코드 중간 문자 마스킹 유틸리티를 설계하고, 마이페이지 항공 및 보험 예약 정보 조회 DTO 매핑부에 이를 적용하여 개인 데이터가 안전하게 가려져 전송되도록 조치했습니다.
  ```java
  // 수정 후 - Response mapping 시 마스킹 유틸리티 적용
  .bookingCode(com.onde.api.utils.MaskingUtils.maskMiddle(fb.getBookingCode()))
  .passengerName(fb.getPassenger() != null ? com.onde.api.utils.MaskingUtils.maskName(fb.getPassenger().getPassengerName()) : null)
  ```

### [8-1] 재고 무한 선점 비즈니스 로직 결함 조치
* **취약점 원인:** 결제를 최종 처리하지 않고 임시 예약 상태(`RESERVED`)로 방치해두어도 데이터베이스의 실재고가 선차감되어, 의도적으로 재고를 모두 고갈시켜 정상 사용자의 예약을 방해할 수 있는 DoS 결함이 존재했습니다.
* **수정된 코드:** [ReservationExpiryScheduler.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/scheduler/ReservationExpiryScheduler.java) (신규), [ReservationRepository.java](file:///c:/Onde/Onde_backend/Onde_Backend/core-module/src/main/java/com/onde/core/repository/ReservationRepository.java)
  * **수정 내용:** 결제 대기 상태로 10분이 지난 숙소 및 렌터카 예약을 매 1분마다 주기적으로 추적하여 예약을 자동 취소(`CANCELLED`)하고, 선점되었던 차감 재고를 안전하게 원래대로 복원시키는 백그라운드 스케줄러 로직을 수립했습니다.
  ```java
  // 수정 후 - 1분 주기 TTL 및 재고 복원 스케줄러 구현
  @Scheduled(cron = "0 */1 * * * *")
  @Transactional
  public void releaseExpiredHolds() {
      LocalDateTime limitTime = LocalDateTime.now().minusMinutes(10);
      List<Reservation> expiredReservations = reservationRepository
              .findByStatusAndCreatedAtBefore(ReservationStatus.RESERVED, limitTime);
      for (Reservation reservation : expiredReservations) {
          reservation.setStatus(ReservationStatus.CANCELLED);
          // 재고 복원 처리 로직...
      }
  }
  ```

### [8-1] 예약 입력값 경계 검증 부재 조치
* **취약점 원인:** 객실 예약 생성 요청 시 예약 인원(`guests`)에 대해 최소 경계 검증이 누락되어 음수 등의 비정상적인 인원 데이터가 예약 파라미터로 유입되는 문제가 있었습니다.
* **수정된 코드:** [RoomReservationRequest.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/accommodation/dto/RoomReservationRequest.java), [ReservationController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/accommodation/ReservationController.java), [AccommodationController.java](file:///c:/Onde/Onde_backend/Onde_Backend/api-module/src/main/java/com/onde/api/application/accommodation/AccommodationController.java)
  * **수정 내용:** DTO의 `guests` 필드에 `@Min(1)` 유효성 검증을 설정하고, 숙소 예약 관련 엔드포인트의 리퀘스트 파라미터 바인딩 직전에 `@Valid` 애너테이션을 선언하여 데이터 경계 처리를 강제화했습니다.
  ```java
  // 수정 후 - DTO 필드 검증 추가
  @Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
  private Integer guests;
  ```
