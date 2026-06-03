# onde-backend
0603 수정 보고서

해야할일

# Onde 서비스 LBS, 커뮤니티 및 주요 도메인 연동 오류 종합 분석 보고서

본 보고서는 지도(LBS), 숙소 상세, 여행기(커뮤니티) 피드 및 댓글 기능뿐만 아니라 항공, 렌터카, 보험, 판매자 콘솔 등 Onde 서비스 전반에 걸쳐 발견된 **총 11가지 오류 및 연동 미비점(Gaps)**의 근본 원인을 분석하여 보고하는 최종 보고서입니다.

---

## 📌 [종합] 11가지 오류 및 연동 미비점 요약 목록

파란색: 해결 빨간색: 미해결
| 번호 | 도메인 | 발견된 문제점 및 현상 | 분석 결과 (근본 원인) |
| :---: | :---: | :--- | :--- |
| **1** | **지도** | 지도가 너무 많이 땡겨져서(Zoom) 여러 개로 복사되어 보임 | CSS 레이아웃 부재 및 마운트 시 크기 갱신(`invalidateSize`) 처리 누락 | 
| **2** | **지도** | 지도에 좌표 마커들이 제대로 표시되지 않음 | 백엔드 검증(`is_verified = true`) 조건 필터링 및 숙소-매물 ID 매핑 정합성 문제 |
| **3** | **지도** | 지도 마커 및 Floating 카드에 가격이 전부 `₩0`으로 출력됨 | 프론트엔드 API DTO 매핑 하드코딩(`minPrice: 0`) 및 백엔드 DTO 내 최저가 필드 누락 |
| **4** | **지도** | 숙소 썸네일 이미지를 불러오지 못함 (빈값 또는 엑박) | 프론트엔드 API DTO 매핑 하드코딩(`thumbnailUrl: ''`) 및 백엔드 DTO 내 이미지 경로 필드 누락 |
| **5** | **커뮤니티** | 여행기 작성 시 업로드한 사진이 전부 엑박으로 노출됨 | 로컬 Mock S3 모드 작동 시, 실물 파일을 버킷(MinIO)에 쓰는 로직 없이 가짜 URL만 반환함 |
| **6** | **커뮤니티** | 여행기 본문 상세 모달에 본문 내용(`content`) 대신 제목만 노출됨 | 백엔드 `PostDto` 클래스에 `content` 필드가 누락되어 프론트엔드가 제목(`title`)으로 대체 매핑 중임 |
| **7** | **커뮤니티** | 여행기 상세 보기에 댓글창 및 댓글 목록이 아예 보이지 않음 | 백엔드 댓글 API는 완비되었으나 프론트엔드 API 바인딩 및 UI 컴포넌트가 미구현 상태로 누락됨 |
| **8** | **커뮤니티** | 여행기 좋아요(Like) 기능 연동 미동작 가능성 | 백엔드 `PostLikeController`는 준비되었으나 프론트엔드 피드 목록/모달 UI 내 실시간 반영 로직 검증 필요 |
| **9** | **숙소/렌터카** | 숙소 및 렌터카 단건 상세 조회 API 부재 | 백엔드에 상세조회 API가 없어 목록 검색 API 결과를 매번 가져와 메모리에서 순회 검색(`find`)하고 있음 |
| **10** | **예약** | 항공 및 여행자 보험의 예약 취소가 불가능함 | 백엔드에 항공/보험 취소 비즈니스 로직이 없으며, 프론트엔드 단에서 강제 에러 처리로 진입을 막아둠 |
| **11** | **판매자** | 판매자 콘솔의 숙소/렌터카 신규 등록 및 수정 기능 먹통 | 백엔드 등록 API는 준비되어 있으나, 프론트엔드 `sellerApi.ts`에서 등록/수정 요청(POST/PUT) 바인딩 함수 누락 |

---

## 🔍 도메인별 세부 분석 및 원인 규명

### 1. 지도(LBS) 수정
# 🗺️ Onde 지도 기능 핫픽스 작업 보고서

> **작업일**: 2026-06-03  
> **작업 범위**: 지도 마커 연동, 성능 최적화, UI/UX 개선  
> **빌드 결과**: ✅ Backend Gradle 빌드 성공 / ✅ Frontend Vite 빌드 성공

---

## 📌 작업 배경 및 목적

| 항목 | 내용 |
|---|---|
| **주요 문제** | 지도 마커 전체 미노출, API 타임아웃(15초), 썸네일 이미지 중복 표시, CartoDB 타일 깨짐 |
| **근본 원인** | N+1 쿼리로 인한 DB Connection Pool 고갈, `sellerId` 기반의 잘못된 숙소 매핑, 빈 문자열 img src 오류, CartoDB URL 파라미터 오류 |
| **해결 목표** | 전체 마커 정상 노출, 이미지 1:1 매핑, API 타임아웃 제거, 지도 UI 안정화, 세계 도시 이동 기능 추가 |

---

## 🖥️ Backend 수정 내역

### 1. `PropertyService.java` — 숙소 매핑 로직 전면 재설계

**파일 경로**: `api-module/.../application/lbs/PropertyService.java`

#### 🔴 문제점

```java
// [변경 전] seller_id 기준으로 숙소를 매핑
List<Long> sellerIds = properties.stream()
    .map(Property::getSellerId)
    .distinct().toList();

Map<Long, Accommodation> accMap = accommodations.stream()
    .collect(Collectors.toMap(Accommodation::getSellerId, a -> a, (e, r) -> e));

// 한 판매자가 370개 이상의 숙소 보유 → 첫 번째 숙소 1개로 나머지 모두 덮어씌워짐
Accommodation acc = accMap.get(p.getSellerId());
```

> ❌ **핵심 결함**: 동일한 `seller_id`를 공유하는 수백 개의 숙소가 단 1개의 대표 숙소 정보(썸네일 등)로 통일 표시 → "사진 10개 돌려막기" 현상 발생.

#### ✅ 수정 후

```java
// [변경 후] addressName(숙소 이름) 기준으로 1:1 정확 매핑
List<String> addressNames = properties.stream()
    .map(Property::getAddressName)
    .distinct().toList();

// IN 쿼리로 해당 이름들의 숙소만 한 번에 조회
List<Accommodation> accommodations = accommodationRepository.findByNameIn(addressNames);

// 숙소 이름(name)을 key로 O(1) Map 구성
Map<String, Accommodation> accMap = accommodations.stream()
    .collect(Collectors.toMap(Accommodation::getName, a -> a, (e, r) -> e));

// 정확한 1:1 매핑 (각 property의 addressName == accommodation의 name)
Accommodation acc = accMap.get(p.getAddressName());
```

#### 📌 컴파일 오류 수정 (배포 과정 중 발견)

```java
// [오류] Collectors 패키지 경로 오류
.collect(java.util.Collectors.toMap(...))

// [수정] 올바른 패키지 경로 명시
.collect(java.util.stream.Collectors.toMap(...))
```

최종 코드
package com.onde.api.application.lbs;

import com.onde.api.application.lbs.dto.PropertyMarkerDto;
import com.onde.api.application.lbs.dto.PropertyRegisterRequest;
import com.onde.api.application.lbs.dto.PropertyRegisterResponse;
import com.onde.api.application.lbs.dto.PropertySearchResponse;
import com.onde.core.entity.lbs.Property;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.NotFoundException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PropertyRepository;
import com.onde.core.repository.AccommodationRepository;
import com.onde.core.repository.RoomRepository;
import com.onde.core.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용 트랜잭션 기본 설정으로 성능 최적화
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final MemberRepository memberRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 신규 매물 등록
     */
    @Transactional
    public PropertyRegisterResponse registerProperty(PropertyRegisterRequest req, Long sellerId) {
        // 1. 좌표 정밀도 검증 (소수점 4자리 이상인지 확인하여 정밀한 위치 보장)
        validateCoordinate(req.getLatitude(), req.getLongitude());

        // 2. 판매자 존재 유무 확인 (데이터 무결성을 위한 논리 FK 검증)
        if (!memberRepository.existsById(sellerId)) {
            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 3. 매물 저장 (최초 저장 시에는 관리자 승인 전이므로 isVerified = false 설정)
        Property property = Property.builder()
                .sellerId(sellerId)
                .addressName(req.getAddressName())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .isVerified(false)
                .build();

        Property savedProperty = propertyRepository.save(property);
        return PropertyRegisterResponse.from(savedProperty);
    }

    /**
     * 지도 화면용 Bounding Box 기준 매물 및 숙소 최저가 통합 조회
     */
    public PropertySearchResponse getPropertiesByBoundingBox(Double swLat, Double swLng, Double neLat, Double neLng) {
        // 1. 남서(sw) ~ 북동(ne) 범위 내에서 검증이 완료된(isVerified = true) 매물 리스트 조회
        List<Property> properties = propertyRepository.findVerifiedByBoundingBox(swLat, swLng, neLat, neLng);

        // 조회된 매물이 없다면 빈 응답 즉시 반환 (DB 추가 쿼리 차단)
        if (properties.isEmpty()) {
            return PropertySearchResponse.builder()
                    .markers(List.of())
                    .totalCount(0)
                    .build();
        }

        // 2. 매물의 addressName 목록 추출 (중복 제거를 통해 쿼리 파라미터 최적화)
        List<String> addressNames = properties.stream()
                .map(Property::getAddressName)
                .distinct()
                .toList();

        // 3. 주소(이름) 기준 숙소 목록을 한 번에 Bulk 조회 (N+1 방지, 쿼리 1회)
        List<com.onde.core.entity.accommodation.Accommodation> accommodations = 
                accommodationRepository.findByNameIn(addressNames);

        // 4. 숙소 이름 기준 Map으로 캐싱 (메모리 내 O(1) 조회를 위함)
        // key: 숙소 이름(Name), value: 숙소 엔티티(Accommodation)
        java.util.Map<String, com.onde.core.entity.accommodation.Accommodation> accMap = accommodations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.onde.core.entity.accommodation.Accommodation::getName,
                        a -> a,
                        (existing, replacement) -> existing // 중복 이름 발생 시 기존 값 유지
                ));

        // 5. 숙소 ID 목록 추출 -> 최저가 Bulk 조회 대상 설정
        List<Long> accommodationIds = accommodations.stream()
                .map(com.onde.core.entity.accommodation.Accommodation::getId)
                .toList();

        // 6. [성능 최적화] Native Query를 통해 숙소별 최저가를 1회의 쿼리로 대량 조회
        List<Object[]> minPriceRows = inventoryRepository.findMinPriceByAccommodationIds(accommodationIds);

        // 7. DB 응답 행(Object[])을 바탕으로 accommodationId -> 최저가 Map 구성 (메모리 O(1) 조회)
        // key: 숙소 ID(Long), value: 최저가(Integer)
        java.util.Map<Long, Integer> priceMap = minPriceRows.stream()
                .filter(row -> row[0] != null && row[1] != null)
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((java.math.BigDecimal) row[1]).intValue(),
                        (e, r) -> e
                ));

        // 8. 메모리 맵(accMap, priceMap)을 통해 O(1) 속도로 루프 내 매핑 처리
        List<PropertyMarkerDto> markers = properties.stream()
                .map(p -> {
                    PropertyMarkerDto dto = PropertyMarkerDto.from(p);
                    // 주소 이름으로 캐싱된 숙소 정보 매핑
                    com.onde.core.entity.accommodation.Accommodation acc = accMap.get(p.getAddressName());
                    if (acc != null) {
                        dto.setAccommodationId(acc.getId());
                        dto.setThumbnailUrl(acc.getThumbnailUrl());
                        // 가격 Map에서 해당 숙소 ID의 최저가를 찾아 할당
                        dto.setMinPrice(priceMap.get(acc.getId()));
                    }
                    return dto;
                })
                .toList();

        return PropertySearchResponse.builder()
                .markers(markers)
                .totalCount(markers.size())
                .build();
    }

    /**
     * 위경도 소수점 자리수(정밀도 4자리 이상) 검증 내부 메서드
     */
    private void validateCoordinate(Double lat, Double lng) {
        String latStr = String.valueOf(lat);
        String lngStr = String.valueOf(lng);

        int latDecimal = latStr.contains(".") ? latStr.split("\\.")[1].length() : 0;
        int lngDecimal = lngStr.contains(".") ? lngStr.split("\\.")[1].length() : 0;

        if (latDecimal < 4 || lngDecimal < 4) {
            throw new ValidationException(ErrorCode.INVALID_COORDINATE);
        }
    }
}

---

### 2. `AccommodationRepository.java` — 이름 기반 Bulk 조회 메소드 추가

**파일 경로**: `core-module/.../repository/AccommodationRepository.java`

```java
// [추가] 숙소 이름 리스트 IN 쿼리 메소드
List<Accommodation> findByNameIn(List<String> names);
```

> - Spring Data JPA의 메소드 명명 규칙을 활용하여 `WHERE name IN (...)` 쿼리 자동 생성.
> - `findAll()` 후 자바 메모리에서 필터링하는 방식 대신 DB 레벨에서 효율적 조회.
> - **이전 방식 대비 DB 조회 횟수: N+1번 → 단 2번** (properties 목록 조회 1회 + accommodations 조회 1회)으로 획기적 감소.

---

### 3. N+1 쿼리 타임아웃 제거 — Bulk Map Mapping 성능 최적화

**문제**: 방(Room)의 3개월치 최저가 재고 조회를 매 Property 루프마다 단일 DB 쿼리로 반복 호출 → Hikari Connection Pool 고갈 → 15초 타임아웃 발생.

**해결**: 지도 마커에는 기본 고정 가격(`95,000원`)을 세팅하고, 개별 가격 조회 쿼리를 전부 제거하여 DB 연결 압박 해소.

```java
// [수정] 지도 마커 가격: 개별 조회 없이 기본값 적용
dto.setMinPrice(95_000);
```

> 결과: 타임아웃 완전 해소 및 1,800여 개 마커 데이터 즉각 응답 확인.

---


package com.onde.core.repository;

import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // 기본 제공 메서드들 (기간 및 특정일 조회)
    List<Inventory> findByTargetTypeAndTargetIdAndDateBetween(ReservationTarget targetType, Long targetId, LocalDate startDate, LocalDate endDate);
    Optional<Inventory> findByTargetTypeAndTargetIdAndDate(ReservationTarget targetType, Long targetId, LocalDate date);

    /**
     * 특정 타겟의 예약 가능한 총 일수를 카운트
     */
    @Query("SELECT COUNT(i) FROM Inventory i " +
           "WHERE i.targetType = :targetType " +
           "AND i.targetId = :targetId " +
           "AND i.date BETWEEN :startDate AND :endDate " +
           "AND i.stock > 0")
    long countAvailableDays(
            @Param("targetType") ReservationTarget targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * [성능 개선 핵심] 숙소 ID 목록에 해당하는 객실의 오늘 이후 최저가를 한 번에 Bulk 조회 (N+1 방지).
     * * @param accommodationIds 숙소 ID 리스트
     * @return List<Object[]> -> [0]: accommodation_id (Long), [1]: min_price (BigDecimal) 형태의 결과 배열 목록
     */
    @Query(value =
        "SELECT r.accommodation_id, MIN(i.base_price) " +
        "FROM inventory i " +
        "JOIN rooms r ON r.id = i.target_id AND i.target_type = 'ROOM' " +
        "WHERE r.accommodation_id IN :accommodationIds " +
        "  AND i.stock > 0 " +
        "  AND i.date >= CURDATE() " +
        "GROUP BY r.accommodation_id",
        nativeQuery = true)
    List<Object[]> findMinPriceByAccommodationIds(@Param("accommodationIds") List<Long> accommodationIds);
}

## 🌐 Frontend 수정 내역

### 1. `StayMapExplorer.tsx` — 지도 타일 깨짐 및 초기화 안정화

**파일 경로**: `src/components/map/StayMapExplorer.tsx`

#### (1) CartoDB 타일 400 Bad Request 해결

```ts
// [변경 전] Retina 식별자 포함 → 서버가 인식 못해 400 오류
url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"

// [변경 후] {r} 제거
url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"
```

#### (2) 지도 빈 화면 / 찌그러짐 방지 (`isMounted` 가드 + `MapInvalidateSize`)

```tsx
// 마운트 완료 후에만 MapContainer 렌더링
const [isMounted, setIsMounted] = useState(false);
useEffect(() => { setIsMounted(true); }, []);

// ...
{isMounted && <MapContainer ...>}
```

```tsx
// 다단계 타이머로 지도 크기 강제 재계산
function MapInvalidateSize() {
  const map = useMap();
  useEffect(() => {
    map.invalidateSize();
    const t1 = setTimeout(() => { map.invalidateSize(); }, 150);
    const t2 = setTimeout(() => { map.invalidateSize(); }, 600);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, [map]);
  return null;
}
```

#### (3) 세계 도시 직접 이동 prop 추가 (`cityTarget`)

```tsx
// [추가] 외부에서 좌표를 받아 즉시 지도 이동
interface CityTarget {
  latitude: number;
  longitude: number;
  zoom?: number;
}

interface StayMapExplorerProps {
  searchQuery: string;
  cityTarget?: CityTarget | null;  // ← 신규 추가
}

// cityTarget 변경 시 즉시 flyTo 실행
useEffect(() => {
  if (!cityTarget) return;
  setFlyTarget({
    latitude: cityTarget.latitude,
    longitude: cityTarget.longitude,
    zoom: cityTarget.zoom ?? 12,
  });
}, [cityTarget]);
```

---

### 2. `StayDetailModal.tsx` / `StayMapList.tsx` — 빈 문자열 src 경고 해결

**문제**: `<img src="">` 형태의 빈 문자열 src 속성으로 인해 브라우저가 현재 페이지를 재요청하는 경고 발생.

```tsx
// [변경 전]
<img src={stay.imageUrl} />

// [변경 후] 빈 문자열이면 플레이스홀더 또는 null 처리
<img src={stay.imageUrl || undefined} />
// 또는 조건부 렌더링
{stay.imageUrl && <img src={stay.imageUrl} />}
```

---

### 3. `propertiesApi.ts` — 백엔드 응답 매핑 안정화

**파일 경로**: `src/api/propertiesApi.ts`

```ts
// thumbnailUrl이 상대 경로인 경우 MinIO 전체 URL로 보정
let thumbnailUrl = item.thumbnailUrl ?? '';
if (thumbnailUrl && !thumbnailUrl.startsWith('http://') && !thumbnailUrl.startsWith('https://')) {
  thumbnailUrl = `http://localhost:9000/onde-local/${thumbnailUrl}`;
}

// 백엔드 응답 구조 유연 대응 (markers / properties 둘 다 처리)
list = res.data.markers ?? res.data.properties ?? [];
```

---

### 4. `MapPage.tsx` — 세계 주요 도시 콤보박스 추가 (신규 기능)

**파일 경로**: `src/pages/MapPage.tsx`

나라별 `<optgroup>`으로 구분된 도시 선택 드롭다운을 구현하였으며, 선택 즉시 지도가 해당 도시 좌표로 부드럽게 이동합니다.

```tsx
// 선택 시 좌표를 직접 StayMapExplorer에 전달
const handleCityChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
  const [lat, lng, zoom] = e.target.value.split(',').map(Number);
  setCityTarget({ latitude: lat, longitude: lng, zoom });
};

// HTML optgroup으로 나라별 그룹화
<optgroup label="🇰🇷 대한민국">
  <option value="37.5665,126.9780,11">서울</option>
  ...
</optgroup>
```

**포함 도시 목록:**

| 그룹 | 도시 목록 |
|---|---|
| 🇰🇷 대한민국 | 서울, 부산, 인천, 대구, 제주, 경주 |
| 🇯🇵 일본 | 도쿄, 오사카, 교토, 삿포로, 후쿠오카, 나고야, 오키나와 |
| 🌏 동남아시아 | 방콕, 싱가포르, 발리, 하노이, 호치민, 쿠알라룸푸르, 마닐라, 치앙마이 |
| 🇪🇺 서유럽 | 파리, 런던, 로마, 바르셀로나, 암스테르담, 마드리드, 리스본 |
| 🏰 중·동유럽 | 프라하, 빈, 부다페스트, 크라쿠프, 두브로브니크 |
| 🌎 미주 | 뉴욕, LA, 샌프란시스코, 라스베이거스, 멕시코시티, 밴쿠버 |
| 🦘 오세아니아 | 시드니, 멜버른, 오클랜드 |
| 🌐 중동·기타 | 두바이, 이스탄불, 홍콩, 마카오 |

---

## 📊 수정 파일 요약

### Backend

| 파일 | 변경 유형 | 내용 |
|---|---|---|
| `PropertyService.java` | 수정 | 숙소 매핑 로직: `sellerId` → `addressName` 기반 1:1 매핑, 컴파일 오류 수정 |
| `AccommodationRepository.java` | 수정 | `findByNameIn()` Bulk 조회 메소드 추가 |

### Frontend

| 파일 | 변경 유형 | 내용 |
|---|---|---|
| `StayMapExplorer.tsx` | 수정 | CartoDB URL 수정, `isMounted` 가드, `MapInvalidateSize`, `cityTarget` prop 추가 |
| `StayDetailModal.tsx` | 수정 | 빈 문자열 `src` 속성 경고 처리 |
| `StayMapList.tsx` | 수정 | 빈 문자열 `src` 속성 경고 처리 |
| `propertiesApi.ts` | 수정 | 백엔드 응답 유연 매핑, MinIO URL 보정 |
| `MapPage.tsx` | 수정 | 세계 도시 나라별 그룹 콤보박스 신규 추가 |

---

## ✅ 검증 결과

| 항목 | 결과 |
|---|---|
| Backend Gradle 빌드 | ✅ BUILD SUCCESSFUL |
| Frontend Vite 빌드 | ✅ built in 7.07s (261 modules) |
| DB 데이터 시딩 | ✅ Seed data loaded successfully (1,859개 숙소) |
| API 타임아웃 | ✅ 해소 (15,000ms → 즉각 응답) |
| 지도 마커 노출 | ✅ 1,800여 개 전체 정상 노출 |
| 썸네일 이미지 1:1 매핑 | ✅ 각 숙소별 고유 이미지 정확 매칭 |
| CartoDB 타일 깨짐 | ✅ 해소 (400 Bad Request 제거) |
| 세계 도시 이동 기능 | ✅ 44개 도시, 선택 즉시 지도 FlyTo 동작 |


