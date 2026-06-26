# Onde 관리자(Admin) 관점 취약점 이행점검 및 코드 수정 가이드

| 항목 | 내용 |
|---|---|
| 기준 보고서 | Onde_관리자_취약점진단보고서_v6(개발관점_28항목).docx (2026-06-23) |
| 기준 가이드라인 | SK Shieldus 2022 Web/API 개발보안 Guideline v3.0.0 (28항목) |
| 대상 시스템 | admin.onde.click 관리자 백오피스 (admin-module) |
| 진단 방식 | Burp Suite Community Edition을 이용한 운영(Production) 환경 수동 진단 |
| 문서 목적 | 발견된 취약점에 대한 **코드 수정(시큐어 코딩) 이행점검 체크리스트** |
| 우선순위 산정 | 영향도(전체 시스템 장악 가능성/금전적 통제 우회) × 악용 난이도 기준 정성 평가. CVSS는 컨설턴트 추정치(참고용) |

> ⚠️ **본 보고서의 특수성**: 관리자 보고서는 28개 항목 중 **8개 항목이 "미진단"**(코드 레벨 점검 미수행) 상태로 명시되어 있습니다. 이 항목들은 "취약 확정"이 아니라 **"후속 진단이 필요한 항목"**이므로, 본 문서에서는 별도로 "사전 점검 가이드"(5장)로 구분하여 다룹니다. 또한 사용자/판매자 보고서와 직접 연계된 침투 체인(1-3 → 4-5)이 존재하므로, 본 문서만으로 조치를 완료할 수 없는 항목은 연계 표시를 했습니다.
>
> 📌 **범위 제외 항목**: 3-5(검색엔진노출), 3-6(백업파일), 7-2(파일목록화), 7-3(서버헤더노출)은 서버/배포 환경 설정 항목으로, 본 개발(코드) 관점 점검 범위에서 제외되어 별도 인프라 트랙에서 다룹니다. 본 문서에는 코드 수정 항목을 포함하지 않습니다.

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

각 항목의 `이행점검` 체크박스는 **① 코드수정 완료 / ② 재현테스트 통과 / ③ 회귀테스트 통과** 3단계로 구성됩니다. "미확정/미진단" 항목은 **① 재검증 수행 → ② 결과 확정 → ③ (취약 확정 시) 코드수정** 순서로 진행합니다.

---

## 2. 전체 현황 요약

| 우선순위 | 항목코드 | 취약점명 | CWE | 추정 심각도 | 상태 |
|---|---|---|---|---|---|
| **P0** | 4-5 | 역할전환 API Mass Assignment (SUPER_ADMIN 임의 할당) | CWE-915 | Critical (9.1) | 취약 |
| **P0** | 4-5 | 정산 최종승인 권한설정 오류 (분리통제 붕괴) | CWE-862 | Critical (8.5) | 취약 |
| **P0** | 4-5 / 1-3(연계) | 일반계정 권한상승 → 관리자API 전체회원탈취 | CWE-862 | Critical (9.0) | 취약 |
| **P1** | 4-3 | 관리자 Health Check 접근제어 우회 (ALB IP 판별 오류) | CWE-284 | High (7.5) | 취약 |
| **P1** | 5-1 | Actuator Health 시스템정보 노출 (admin) | CWE-200 | High (7.1) | 취약 |
| **P1** | 3-2 | 관리자 로그인 무제한 브루트포스 | CWE-307 | High (7.5) | 취약 |
| **P2** | 6-1 | GlobalExceptionHandler 설계 결함 (admin-module) | CWE-209 | Medium (5.3) | 취약 |
| **P2** | 1-2 | 관리자 검색 API Injection (Tomcat 레벨 차단만 확인) | CWE-89 | 잠재 Critical | 미확정 |
| **P2** | 2-1 | SVG 썸네일 관리자 승인화면 렌더링 방식 | CWE-79 | 잠재 High | 미확정 |
| **P2** | 8-1 | Admin API 인코딩/헤더 우회 시도 | CWE-284 | 잠재 High | 미확정 |
| **P2** | 3-4 | 관리자 도메인명 추측 가능 | CWE-200 | Low | 주의 |
| **P3** | 1-4, 1-5, 2-2, 4-1, 4-2, 5-2, 6-2, 7-4 | (8개 항목) 미진단 — 사전 점검 필요 | - | 미진단 | 미진단 |

조치 불필요(양호/해당없음) 항목은 **6장**, 범위 제외(인프라 트랙) 항목은 **7장**에 기록합니다.

---

## 3. P0 — Critical (즉시조치)

### [4-5] 역할전환 API의 임의 Role 할당 (Mass Assignment)

- **CWE**: CWE-915
- **대상**: 관리자 "회원 → SELLER 전환" API
- **공격 재현**: 전환 API의 `roles` 필드에 의도된 값(`SELLER`)이 아닌 `SUPER_ADMIN`을 주입 → 200 OK "권한이 수정되었습니다." + DB의 role 컬럼이 실제로 `SUPER_ADMIN`으로 변경됨 확인.
- **근본 원인**: "SELLER로 전환"이라는 좁은 목적의 API가 `roles` 필드를 화이트리스트(SELLER 단일 값)로 제한하지 않고 클라이언트가 보낸 임의의 역할 문자열을 그대로 수용. 사용자 보고서 1-3(회원가입 시 role 직접 주입)과 **동일한 근본 원인 패턴**(서버 전역의 role 값 화이트리스트 검증 부재)이 관리자 화면에도 존재.

**Before (취약 패턴)**
```java
public class RoleChangeRequest {
    private List<String> roles; // 클라이언트가 임의 값 주입 가능
}

member.setRoles(request.getRoles()); // 검증 없이 그대로 적용
```

**After (수정안)**
```java
// 이 API의 목적은 "SELLER 전환" 단일 기능이므로 클라이언트로부터 role 값 자체를 받지 않음
@PatchMapping("/admin/members/{memberId}/seller-conversion")
@PreAuthorize("hasRole('SUPER_ADMIN')") // 누가 이 전환을 수행할 수 있는지도 함께 점검 필요
public ResponseEntity<?> convertToSeller(@PathVariable Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException());
    member.setRole(Role.SELLER); // 서버가 고정값 부여, 클라이언트 입력 무시
    auditLogService.record(memberId, "ROLE_CHANGE_TO_SELLER"); // 권한 변경은 반드시 감사로그 기록
    return ResponseEntity.ok(ApiResponse.success("SELLER로 전환되었습니다."));
}
```

**근본 원인 재발 방지 권고**: `SUPER_ADMIN` 등 고위 권한 부여는 이 API와 **완전히 분리된 별도 절차**(예: DB 직접 작업 + 2인 승인, 또는 별도의 격리된 관리 콘솔)로만 가능하도록 제한해야 합니다. "역할 변경"을 다루는 모든 API에서 클라이언트가 보낸 role 문자열을 그대로 신뢰하는 코드가 있는지 전수 점검을 권고합니다.

**이행점검**
- [ ] roles 필드 제거 및 서버 측 SELLER 고정 로직 적용
- [ ] SUPER_ADMIN 부여 절차를 별도 분리(이 API와 무관하게)
- [ ] 역할 변경 시 감사로그(Audit Log) 기록 추가
- [ ] Burp Repeater로 `roles:["SUPER_ADMIN"]` 주입 PoC 재전송 → SELLER로 강제 처리 확인

---

### [4-5] 정산 최종승인 권한설정 오류 (분리통제 붕괴)

- **CWE**: CWE-862 (Broken Authorization)
- **엔드포인트**: `POST /admin-api/api/v1/admin/settlements/{id}/approve`
- **공격 재현**: 의도된 정상 흐름은 SELLER_ADMIN의 1차 요청 → SUPER_ADMIN만의 최종 승인(`finalize`)이나, `approve` 엔드포인트의 권한 설정이 SUPER_ADMIN과 SELLER_ADMIN 모두를 허용하여 **SELLER_ADMIN이 직접 최종 승인까지 도달**(`status:COMPLETED`)함이 확인됨. 본인이 1차로 올린 정산 건을 본인이 최종 확정할 수 있어 금전적 통제 우회로 이어질 수 있음.
- **참고(조치 불필요 부분)**: 금액(`amount`) 필드는 변조 시(예: `-50000`) 서버가 자체 계산값으로 덮어써 안전함이 확인됨. 동시 승인 요청(Race Condition) 테스트에서도 13개 동시 요청 중 1건만 처리되어 **이중 승인은 발생하지 않음**(안전). 단, 나머지 12건이 500 Internal Server Error로 응답된 점은 별도 결함(6-1 참조).

**Before (취약 패턴)**
```java
@PreAuthorize("hasAnyRole('SUPER_ADMIN','SELLER_ADMIN')") // 모든 상태 전환에 동일하게 적용됨 — 문제의 원인
@PostMapping("/settlements/{id}/approve")
public ResponseEntity<?> approve(@PathVariable Long id) {
    Settlement s = settlementRepository.findById(id).orElseThrow();
    if (s.getStatus() == SettlementStatus.PENDING) {
        s.setStatus(SettlementStatus.APPROVED_1ST);
    } else if (s.getStatus() == SettlementStatus.APPROVED_1ST) {
        s.setStatus(SettlementStatus.COMPLETED); // 권한 구분 없이 누구나 도달 가능
    }
}
```

**After (수정안 — 상태머신 + 단계별 역할 분리)**
```java
@PostMapping("/settlements/{id}/approve")
public ResponseEntity<?> approve(@AuthenticationPrincipal AdminPrincipal admin,
                                  @PathVariable Long id) {
    Settlement s = settlementRepository.findById(id).orElseThrow();

    switch (s.getStatus()) {
        case PENDING -> {
            requireRole(admin, Role.SELLER_ADMIN, Role.SUPER_ADMIN); // 1차 요청: 양쪽 허용
            s.setStatus(SettlementStatus.APPROVED_1ST);
        }
        case APPROVED_1ST -> {
            requireRole(admin, Role.SUPER_ADMIN); // 최종 승인: SUPER_ADMIN 단독
            s.setStatus(SettlementStatus.COMPLETED);
        }
        default -> throw new InvalidSettlementStateException();
    }
    auditLogService.record(admin, s, "SETTLEMENT_STATUS_CHANGE");
    return ResponseEntity.ok(SettlementResponse.from(s));
}

private void requireRole(AdminPrincipal admin, Role... allowed) {
    if (Arrays.stream(allowed).noneMatch(admin::hasRole)) {
        throw new AccessDeniedException("해당 단계를 처리할 권한이 없습니다.");
    }
}
```

**낙관적 락으로 동시성 충돌을 명시적 예외로 전환 (500 → 409, 6-1과 연동)**
```java
@Entity
public class Settlement {
    @Version
    private Long version; // 동시 승인 시도 시 OptimisticLockException 발생 → 의도된 409로 변환
}
```

**이행점검**
- [ ] 상태별(PENDING/APPROVED_1ST) 역할 분리 적용 — 최종 승인은 SUPER_ADMIN 단독
- [ ] 정산 상태 변경 시 감사로그(누가 1차 요청/누가 최종 승인했는지) 기록
- [ ] `@Version` 낙관적 락 적용 + 동시성 충돌 시 409 응답으로 전환
- [ ] Burp Repeater로 SELLER_ADMIN 토큰 + approve(APPROVED_1ST 상태) PoC 재전송 → 403 확인
- [ ] Turbo Intruder 동시요청 재현 → 1건 200 + 나머지 409(500 아님) 확인

---

### [4-5 / 1-3 연계] 일반계정 권한상승 → 관리자 API 전체회원 탈취

- **CWE**: CWE-862
- **엔드포인트**: `GET /admin-api/api/v1/admin/members`
- **공격 체인**: 사용자 보고서 1-3(회원가입 시 `role:SUPER_ADMIN` 강제 주입) → 해당 토큰으로 본 엔드포인트 접근 → 200 OK + 전체 회원 목록(memberId, email, role, status) 탈취.
- **중요**: 일반(비관리자) 토큰 및 HTTP 메서드 변조(PATCH)로의 접근은 이미 403으로 정상 차단됨이 확인되었습니다(`(참고) 관리자 API 수직 권한 상승 검증` — 양호). 즉 **권한 검증 로직 자체는 정상 동작**하며, 문제는 "role 값이 애초에 불법적으로 SUPER_ADMIN이 될 수 있다"는 상류(upstream) 결함입니다.

**조치 방향 — 단일 지점 의존 제거(다층 방어)**

이 항목은 코드 한 곳을 고친다고 끝나지 않습니다. SUPER_ADMIN 권한이 생성/부여되는 모든 경로(회원가입 1-3, 역할전환 4-5 Mass Assignment)를 막아야 다시 발생하지 않습니다. 추가로 권한 검증 로직 자체에 의존하지 않는 탐지 계층을 둘 것을 권고합니다.

```java
// 권한 상승 탐지: role이 SUPER_ADMIN으로 "변경"되는 모든 지점에 이벤트 발행
@EventListener
public void onRoleEscalation(RoleChangedEvent event) {
    if (event.getNewRole() == Role.SUPER_ADMIN) {
        alertService.notifySecurityTeam(
            "SUPER_ADMIN 권한 부여 발생: memberId=" + event.getMemberId()
            + ", 변경경로=" + event.getSourceEndpoint()
        );
        // 즉시 알림으로 비정상 경로를 통한 권한 부여를 신속히 포착
    }
}
```

**이행점검**
- [ ] 사용자 보고서 1-3(회원가입 role 고정) 조치 완료 확인
- [ ] 본 문서 4-5(역할전환 Mass Assignment) 조치 완료 확인
- [ ] SUPER_ADMIN 권한 부여 이벤트에 대한 감사로그/알림 추가 (다층 방어)
- [ ] 1-3 + 4-5 조치 완료 후 전체 체인 재현 테스트(회원가입 → 관리자API 접근) → 차단 확인

---

## 4. P1 — High (이번 스프린트 내 조치)

### [4-3] 관리자 Health Check 접근제어 우회 (ALB 구조상 IP 판별 오류)

- **CWE**: CWE-284
- **엔드포인트**: `GET /api/v1/admin/health`
- **현황**: `IpAddressMatcher` 기반 IP 화이트리스트가 설계되어 있으나, ALB 뒤에서는 `request.getRemoteAddr()`가 ALB의 내부 IP를 반환하는 구조적 문제로 IP 기반 통제가 사실상 무력화됨. Authorization/Cookie 없이 200 OK + Actuator Health 데이터가 그대로 반환되며, 응답의 `diskSpace.path`가 `C:\Windows\system32\`로 노출되어 admin-module이 Windows EC2에서 구동 중임이 외부에서 직접 확인됨. Host 헤더 불일치 요청도 그대로 처리되어 검증이 느슨함.

**Before (취약 패턴)**
```java
IpAddressMatcher matcher = new IpAddressMatcher(allowedCidr);
if (!matcher.matches(request.getRemoteAddr())) { // ALB 뒤에서는 항상 ALB 내부 IP를 반환
    throw new AccessDeniedException();
}
```

**After (권장: IP 기반 통제를 인증/인가 기반으로 전환)**
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/v1/admin/health")
public ResponseEntity<?> health() {
    return ResponseEntity.ok(healthIndicator.health());
}
```

**병행 조치 — 응답 상세정보 차단**
```yaml
# application.yml (admin-module)
management:
  endpoint:
    health:
      show-details: never   # diskSpace, OS 등 인프라 세부정보 응답에서 제거
```

**IP 기반 통제를 유지해야 하는 경우(대안)**: `ForwardedHeaderFilter` 등록 또는 WAS(Tomcat) `RemoteIpValve` 설정으로 `X-Forwarded-For`를 신뢰할 수 있는 체인(ALB만)에서 파싱하도록 보정해야 합니다. 단, 인증/인가 기반 전환이 더 근본적인 해결책입니다.

**이행점검**
- [ ] IP 기반 통제 → `@PreAuthorize("hasRole('ADMIN')")` 전환 (또는 X-Forwarded-For 파싱 보정)
- [ ] `management.endpoint.health.show-details=never` 적용
- [ ] Host 헤더 검증 로직 추가(예상 도메인 불일치 시 차단)
- [ ] Burp Repeater로 Authorization 없는 PoC 재전송 → 401/403 확인

---

### [5-1] Actuator Health를 통한 시스템 정보 노출 (관리자)

- **CWE**: CWE-200
- **현황**: DB 종류(MariaDB), Redis 버전(7.1.0), 디스크 경로 등 내부 시스템 세부정보가 Health 응답에 포함되어 정찰(reconnaissance) 단계를 지원함. (근거는 4-3과 동일 응답)

```java
// Spring Security 설정에서 /actuator/** 전체를 ADMIN 권한으로 제한 (ALB 라우팅에만 의존하지 않음)
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/actuator/**").hasRole("ADMIN")
        .anyRequest().authenticated();
}
```
- 4-3에서 적용한 `show-details: never`가 본 항목의 근본 조치이기도 합니다. **4-3과 동시에 조치**하십시오.

**이행점검**
- [ ] `/actuator/**` 경로에 Spring Security 인가 규칙 추가 (ALB 라우팅 누락 시에도 코드 레벨에서 차단)
- [ ] 4-3 조치(show-details: never)와 함께 검증
- [ ] Burp Repeater로 DB/Redis 버전 노출 여부 재확인

---

### [3-2] 관리자 로그인 무제한 브루트포스

- **CWE**: CWE-307
- **엔드포인트**: `POST /user-api/api/v1/auth/admin/login`
- **현황**: 사용자 보고서 3-2와 동일한 결함이 관리자 로그인에도 존재. 관리자 계정 탈취는 4-5(권한상승) 체인과 직결되어 영향도가 더 큼.

```java
// 관리자 로그인은 일반 로그인보다 더 엄격한 기준 적용
long attempts = redisTemplate.opsForValue().increment("admin_login_attempt:" + email, 1);
if (attempts == 1) redisTemplate.expire(key, Duration.ofMinutes(30));
if (attempts > 3) { // 일반 로그인(5회)보다 낮은 임계치
    throw new TooManyAttemptsException("관리자 계정 보호를 위해 로그인이 일시 제한됩니다.");
}
```
- CAPTCHA를 **필수**로 적용하고, 2단계 인증(OTP) 도입을 검토 권고합니다(금전 통제권을 가진 계정이므로 일반 사용자보다 보호 수준을 높여야 함).

**이행점검**
- [ ] 관리자 로그인에 더 엄격한 Rate Limiting(3회) 적용
- [ ] CAPTCHA 필수 적용
- [ ] 2FA(OTP) 도입 검토 결과 기록
- [ ] Burp Intruder로 동일 PoC 재실행 → 차단 확인

---

## 5. P2 — Medium (재검증 필요 / 일반 항목)

### [6-1] GlobalExceptionHandler 설계 결함 (admin-module)

- **CWE**: CWE-209 | **현황**: 사용자 보고서 6-1과 동일한 구조적 결함이 admin-module에도 존재. `Content-Type: text/xml` 등 다수의 4xx 예외가 500으로 처리되며 `systemMessage`에 Spring 클래스 경로 노출. 4-5의 동시성 충돌(500 Internal Server Error) 역시 같은 구조적 문제의 증거.

```java
// api-module과 admin-module의 GlobalExceptionHandler를 core-module 공통으로 통합
@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(415).body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ErrorResponse.of("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(OptimisticLockException.class) // 4-5 동시성 충돌 전용 매핑
    public ResponseEntity<ErrorResponse> handle(OptimisticLockException e) {
        return ResponseEntity.status(409).body(ErrorResponse.of("CONFLICT", "이미 처리된 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500).body(ErrorResponse.of("INTERNAL_ERROR", "처리 중 오류가 발생했습니다."));
    }
}
```
`application.yml`(프로덕션 프로필): `server.error.include-stacktrace: never`

**이행점검**
- [ ] api-module/admin-module GlobalExceptionHandler를 core-module로 통합
- [ ] 예외 타입별 4xx 매핑 재설계 (특히 OptimisticLockException → 409)
- [ ] systemMessage 프로덕션 응답에서 제거
- [ ] Burp Repeater로 `Content-Type: text/xml` PoC 재전송 → 415 확인

---

### [1-2] 관리자 검색 API Injection — 재검증 필요 (미확정)

- **CWE**: CWE-89 | **엔드포인트**: `GET /api/v1/admin/members?keyword=`
- **현황**: boolean-based 페이로드(`') OR ('1'='1`)가 **Tomcat 컨테이너 레벨**에서 400으로 차단됨이 확인됨. 이는 "애플리케이션이 안전하게 처리했다"는 의미가 아니며, 요청이 컨트롤러에 도달하기 전 차단된 것입니다. 사용자 보고서 1-2에서 동일 플랫폼의 다른 엔드포인트가 이미 UNION Injection으로 확정된 사례가 있어 추가 검증이 반드시 필요합니다.

**재검증 절차(우선 수행)**
1. 동일 페이로드를 URL 인코딩(`%27%29%20OR%20%28%271%27%3D%271`)하여 Tomcat 레벨 차단 우회
2. 우회 성공 시 실제 컨트롤러/Repository의 파라미터 바인딩 방식 확인

**사전 조치(검증 결과와 무관하게 적용 권장)**
```java
@Query("SELECT m FROM Member m WHERE m.name LIKE %:keyword% OR m.email LIKE %:keyword%")
List<Member> searchByKeyword(@Param("keyword") String keyword); // JPA 파라미터 바인딩 강제
```

**이행점검**
- [ ] URL 인코딩 우회 재검증 테스트 수행
- [ ] 재검증 결과에 따라 상태 확정(양호/취약)
- [ ] JPA 파라미터 바인딩 사전 적용 (재검증 결과 대기 없이 즉시 가능)
- [ ] (취약 확정 시) 사용자 보고서 1-2와 동일한 일괄 점검 수행

---

### [2-1] SVG 썸네일 관리자 승인화면 렌더링 — 재검증 필요 (미확정)

- **CWE**: CWE-79 | **현황**: SVG(`<script>` 포함) 썸네일이 검증 없이 저장되는 것은 사용자 보고서 2-1에서 확정됨. 관리자 승인화면이 이를 `<img src>`로만 렌더링하면 스크립트가 실행되지 않으나, **새 탭에서 원본을 직접 여는 기능이 있다면** 관리자 권한 컨텍스트에서 스크립트가 실행될 위험(영향도가 사용자 측보다 큼 — 관리자 권한 탈취로 이어질 수 있음).

**재검증 절차**: 관리자 숙소 승인 화면의 프론트엔드 코드에서 썸네일 표시 방식(`<img>` 단순 렌더링 vs `<object>`/새 탭 직접 열기) 확인.

**사전 조치(권장)**
```java
// S3 업로드 시 모든 사용자 업로드 파일에 Content-Disposition: attachment 강제
// → 어떤 방식으로 링크를 열어도 "다운로드"만 되고 브라우저에서 직접 렌더링/실행되지 않음
PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentDisposition("attachment")
        .build();
```
- 대안: 사용자 업로드 콘텐츠를 관리자 메인 도메인과 분리된 격리 도메인(예: `usercontent.onde.click`)에서 서빙하여 Origin을 분리.

**이행점검**
- [ ] 관리자 승인화면 썸네일 렌더링 방식 코드 확인
- [ ] Content-Disposition: attachment 적용 (또는 격리 도메인 분리)
- [ ] 재검증: SVG(`<script>`) 썸네일을 관리자 계정으로 새 탭에서 열어 실행 여부 확인

---

### [8-1] Admin API 인코딩/헤더 우회 시도 — 재검증 필요 (미확정)

- **CWE**: CWE-284 | **현황**: URL 인코딩(`%61dmin`) 및 `Host`/`X-Custom-Ip-Authorization` 헤더 위조 시도가 200 OK를 반환했으나, 테스트에 사용된 토큰이 이미 SUPER_ADMIN 권한이었기 때문에 "우회 성공"으로 결론지을 수 없음. 일반 USER 토큰 또는 무인증 상태로 동일 기법을 재시도하는 비교 테스트가 빠져 있음.

**재검증 절차**: 일반 USER 토큰 및 무인증 상태로 동일한 URL 인코딩/헤더 위조 기법 재시도 → 200 OK가 나오면 실제 우회 확정.

**사전 조치(권장)**
```java
// 클라이언트가 보낼 수 있는 커스텀 헤더(X-Custom-Ip-Authorization 등)로 권한을 판단하는 로직이
// 있다면 즉시 제거. 권한 판단은 오직 서버가 검증한 JWT의 role 클레임으로만 수행해야 함.
```

**이행점검**
- [ ] 일반 USER 토큰으로 동일 기법 재검증 수행
- [ ] 커스텀 헤더 기반 인가 로직 존재 여부 코드 점검 및 제거
- [ ] 재검증 결과에 따라 상태 확정

---

### [3-4] 관리자 도메인명 추측 가능

- 사용자/판매자 보고서 3-4와 **동일한 진단 결과**이므로 중복 조치는 불필요합니다. 도메인명 변경은 코드 영역이 아닌 배포/DNS 설정 사항이므로 인프라팀에 별도 전달하고, 코드 레벨에서는 robots.txt에 관리자 경로 Disallow 설정만 추가하면 됩니다(사용자 보고서 가이드의 robots.txt 항목 참조).

**이행점검**
- [ ] robots.txt에 admin 경로 Disallow 반영 (사용자보고서 문서와 중복 조치 1회만 수행)
- [ ] 도메인명 변경 검토 사항을 별도 트랙(인프라/배포)에 전달 확인

---

## 6. P3 — 미진단 항목 사전 점검 가이드

아래 8개 항목은 보고서에서 **"미진단"**으로 명시된 항목입니다. 즉 코드 레벨 점검이 아직 수행되지 않았으므로, "취약"이 확정된 것이 아니라 **우선순위 높은 후속 진단 대상**입니다. 코드 수정에 앞서 먼저 점검을 수행하고, 발견 시 기존 조치(사용자 보고서의 동일 항목 패턴)를 그대로 적용하면 됩니다.

| 항목코드 | 점검 대상 | 점검 방법 | 발견 시 적용할 조치 |
|---|---|---|---|
| 1-4 | admin-module 자체 기능 중 외부 URL/파일경로 입력 기능(로고 업로드, 리포트 export 등) | 전수 조사 후 1-4(사용자보고서) PoC 패턴으로 재현 | 본 시리즈 사용자보고서 1-4와 동일한 화이트리스트(템플릿 enum, 도메인 화이트리스트) 적용 |
| 1-5 | admin-module의 redirect_uri/콜백 파라미터 | 임의 외부 URL로 변조 후 Location 헤더 확인 | 사전 등록된 URI 고정 검증 로직 적용 |
| 2-2 | admin-module 파일 다운로드 기능 존재 여부 | 경로 조작(`../`) 페이로드로 Path Traversal 테스트 | 인덱싱 값/화이트리스트 기반 참조로 전환 |
| 4-1 | AdminJwtAuthenticationFilter가 다루는 쿠키/스토리지 | Set-Cookie 헤더의 Secure/HttpOnly/SameSite 속성 확인 | 사용자보고서 4-1과 동일하게 SameSite=Strict/Lax 적용 |
| 4-2 | AdminJwtAuthenticationFilter의 토큰 발급/검증 로직 | JWT payload 디코딩, 만료시간, alg 변조 테스트 | 사용자보고서 4-2와 동일하게 payload 최소화(memberId 기반) |
| 5-2 | 관리자 회원 상세조회/정산 상세조회 응답 | 응답 바디에 평문 민감정보(연락처, 계좌 등) 포함 여부 확인 | 사용자보고서 5-2와 동일하게 마스킹 처리 적용 |
| 6-2 | admin-module 에러 응답 포맷 일관성 | 여러 에러 유형(400/404/500)의 JSON 구조 비교 | 6-1(본 문서) core-module 통합 작업과 함께 진행 |
| 7-4 | admin.onde.click 응답 헤더 (CSP/HSTS) | 응답 헤더에서 CSP/HSTS 존재 여부 확인 | 사용자보고서 7-4와 동일한 nginx 헤더 추가 (관리자 도메인에도 동일 적용) |

**이행점검**
- [ ] 8개 항목 점검 일정 수립 및 수행
- [ ] 발견된 취약점은 본 문서 또는 사용자보고서의 동일 패턴 조치로 즉시 반영
- [ ] 점검 결과를 보고서 v2에 "미진단" → "양호/취약" 확정 상태로 갱신

---

## 7. 범위 제외 항목 (서버/배포 환경 — 별도 인프라 트랙)

아래 항목은 애플리케이션 코드 레벨이 아닌 서버/배포 환경 설정 사항으로, 본 개발 관점 점검 범위에서 제외되었습니다. 코드 수정 대상이 아니므로 본 문서에서는 다루지 않으며, 인프라 담당 트랙에서 별도로 진행해야 합니다.

- 3-5 검색엔진 정보 노출 가능성
- 3-6 백업 파일 및 테스트 파일 존재 여부
- 7-2 파일 목록화 가능성
- 7-3 서버 헤더정보 노출

---

## 8. 조치 불필요 (양호/해당없음) — 회귀 방지 기록

| 항목 | 확인된 안전 동작 |
|---|---|
| 1-1 | 관리자 회원/숙소 목록의 텍스트 필드는 React JSX 자동 escape로 Stored XSS 차단 (8-1 참조) |
| 1-6 | 정산 승인 금액(amount) 변조 시 서버가 자체 계산값 사용, DB 원본 유지 |
| 3-1 / 3-3 | 사용자/판매자 보고서에서 다룸 (관리자 화면에 해당 없음) |
| (참고) | 일반 토큰의 관리자 API 수직 권한상승 시도 403 정상 차단, HTTP 메서드(PATCH) 변조로도 우회되지 않음 |
| 4-4 | Authorization 헤더 제거 시 401 정상 차단 (4-3의 IP 우회 문제와는 별개 — 4-3 취약 판정 상쇄하지 않음) |
| 7-1 | TRACE 메서드 ALB 단에서 405로 정상 차단 |
| 7-4 | 관리자 목록 API page/size 파라미터 경계값(음수/초과값) 정상 clamp 처리 |
| 8-1 | XXE — XML 컨버터 미등록으로 구조적으로 불가능 |
| 8-1 | OAuth2 state CSRF 검증 정상 |
| 8-1 | redirect_uri 변조 시 사전 등록 URI로 고정됨 |
| 8-1 | Swagger/OpenAPI 비노출 |
| 8-1 | Actuator 표준 엔드포인트(beans, mappings 등) 비노출 (Health 제외 — 5-1 별도 조치) |
| 8-1 | 관리자 예약취소 API — SUPER_ADMIN의 임의 예약 취소는 정상 설계(IDOR 오탐 아님) |

---

## 9. 부록 — 사용자/판매자 보고서 연계 조치 매핑

| 본 문서 항목 | 연계된 사용자보고서 항목 | 선후관계 |
|---|---|---|
| 4-5 (일반계정 권한상승) | 1-3 (회원가입 role 강제 주입) | 1-3 선조치 필요, 4-5는 2차 방어선(감사로그) |
| 4-5 (역할전환 Mass Assignment) | 1-3 (동일 근본원인 패턴) | 독립 조치이나 동일 패턴이므로 동시 점검 권장 |
| 3-2 (관리자 로그인 브루트포스) | 3-2 (일반 로그인 브루트포스) | 동일 메커니즘, 관리자는 더 엄격한 기준 적용 |
| 3-4 (관리자 도메인명) | 3-4 (동일 진단결과) | 중복 조치 불필요, 1회만 수행 |
| 6-1 (admin-module 에러처리) | 6-1 (api-module 에러처리) | core-module 통합으로 동시 조치 권장 |

---

## 10. 서명/완료 확인

| 구분 | 담당자 | 완료일 | 비고 |
|---|---|---|---|
| 코드 수정 (P0) | | | |
| 코드 수정 (P1) | | | |
| 코드 수정 (P2, 재검증 포함) | | | |
| 미진단 항목 점검(P3) | | | |
| 코드 리뷰 | | | |
| 재현 테스트(PoC 재실행) | | | |
| 회귀 테스트 | | | |
| 사용자보고서 연계 항목 교차 확인 | | | |
| 보고서 v2 갱신 | | | |
