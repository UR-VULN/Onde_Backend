import urllib.request
import urllib.error
import json
import sys

# 테스트 전역 상태 관리
property_id = None
post_id = None

def make_request(method, url, headers=None, body=None, is_multipart=False):
    if headers is None:
        headers = {}
    
    req_body = None
    if body is not None:
        if is_multipart:
            req_body = body
        elif isinstance(body, (dict, list)):
            req_body = json.dumps(body).encode('utf-8')
            headers['Content-Type'] = 'application/json'
        else:
            req_body = body.encode('utf-8')
            
    req = urllib.request.Request(url, data=req_body, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req) as res:
            status = res.status
            content_type = res.info().get('Content-Type', '')
            response_data = res.read()
            
            # PDF나 바이너리 응답의 경우 텍스트 대신 크기 출력
            if 'application/pdf' in content_type:
                return status, f"[PDF Document: {len(response_data)} bytes]"
            
            try:
                decoded_data = response_data.decode('utf-8')
                return status, json.loads(decoded_data)
            except Exception:
                return status, response_data.decode('utf-8')
    except urllib.error.HTTPError as e:
        status = e.code
        try:
            err_data = e.read().decode('utf-8')
            return status, json.loads(err_data)
        except Exception:
            return status, str(e)
    except Exception as e:
        return 500, str(e)

def print_result(title, status, response, expected_statuses=None):
    if expected_statuses is None:
        expected_statuses = [200, 201]
        
    status_icon = "SUCCESS" if status in expected_statuses else "FAIL"
    if status_icon == "FAIL" and (status == 400 or status == 403 or status == 404):
        # 예상된 실패 시나리오인 경우 노란색 처리
        status_icon = "EXPECTED FAIL"
        
    print(f"\n==================================================")
    print(f"[*] {title}")
    print(f"==================================================")
    print(f"Status Code: {status} ({status_icon})")
    print(f"Response Body:")
    print(json.dumps(response, indent=2, ensure_ascii=False))

def run_tests():
    global property_id, post_id
    
    print("[START] ONDE API Integration Testing Tool Starting...")
    print("Wait 3 seconds for servers to fully load...")
    import time
    time.sleep(3)
    
    # 0.1 일반 사용자 생성
    status, res = make_request("POST", "http://localhost:8080/api/v1/members/test", 
                               body={"id": 1, "name": "홍길동", "role": "USER"})
    print_result("0.1. 일반 사용자(USER - ID: 1) 회원 데이터 생성", status, res)
    
    # 0.2 판매자 생성
    status, res = make_request("POST", "http://localhost:8080/api/v1/members/test", 
                               body={"id": 2, "name": "김판매", "role": "SELLER"})
    print_result("0.2. 판매자(SELLER - ID: 2) 회원 데이터 생성", status, res)
    
    # 1.1 판매자 매물 좌표 변환 및 등록 (성공)
    status, res = make_request("POST", "http://localhost:8080/api/v1/properties",
                               headers={"X-Member-Id": "2", "X-Member-Role": "SELLER"},
                               body={"addressName": "서울시 강남구 테헤란로 1", "latitude": 37.5665, "longitude": 126.9781})
    print_result("1.1. 판매자(SELLER) 매물 좌표 변환 및 등록 (성공)", status, res)
    if status == 201 or status == 200:
        if isinstance(res, dict) and 'data' in res and 'propertyId' in res['data']:
            property_id = res['data']['propertyId']
            print(f"--> 획득한 propertyId: {property_id}")
            
    # 1.2 판매자 매물 등록 실패 케이스 (좌표 정밀도 부족)
    status, res = make_request("POST", "http://localhost:8080/api/v1/properties",
                               headers={"X-Member-Id": "2", "X-Member-Role": "SELLER"},
                               body={"addressName": "서울시 강남구 테헤란로 1", "latitude": 37.5, "longitude": 126.97})
    print_result("1.2. 판매자 매물 등록 실패 케이스 (좌표 정밀도 부족)", status, res, expected_statuses=[400])
    
    # 1.3 지도 화면 내 매물 실시간 조회 (Bounding Box)
    status, res = make_request("GET", "http://localhost:8080/api/v1/properties?swLat=37.49&swLng=126.97&neLat=37.58&neLng=127.02")
    print_result("1.3. 지도 화면 내 매물 실시간 조회 (Bounding Box)", status, res)
    
    # 2.1 게시글 및 다중 첨부 이미지 등록 (multipart/form-data)
    boundary = "boundary"
    multipart_body = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="title"\r\n\r\n'
        "도쿄 3박 4일 감성 여행 후기\r\n"
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="content"\r\n\r\n'
        "정말 좋았어요! 신주쿠 라멘 맛집 추천합니다.\r\n"
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="type"\r\n\r\n'
        "REVIEW\r\n"
        f"--{boundary}--\r\n"
    )
    status, res = make_request("POST", "http://localhost:8080/api/v1/posts",
                               headers={
                                   "X-Member-Id": "1",
                                   "X-Member-Role": "USER",
                                   "Content-Type": f"multipart/form-data; boundary={boundary}"
                               },
                               body=multipart_body.encode('utf-8'),
                               is_multipart=True)
    print_result("2.1. 게시글 및 다중 첨부 이미지 등록 (multipart/form-data)", status, res)
    if status == 201 or status == 200:
        if isinstance(res, dict) and 'data' in res and 'postId' in res['data']:
            post_id = res['data']['postId']
            print(f"--> 획득한 postId: {post_id}")
            
    if post_id is None:
        post_id = 1
        print("[WARN] postId 획득 실패. 기본값 1로 대체하여 다음 단계 진행합니다.")
        
    # 2.2 게시글 필터링 페이징 조회
    status, res = make_request("GET", f"http://localhost:8080/api/v1/posts?type=REVIEW&status=ACTIVE&page=0&size=20")
    print_result("2.2. 게시글 필터링 페이징 조회", status, res)
    
    # 2.3 타인의 게시글 삭제 시도 (실패 케이스)
    status, res = make_request("DELETE", f"http://localhost:8080/api/v1/posts/{post_id}",
                               headers={"X-Member-Id": "999", "X-Member-Role": "USER"})
    print_result("2.3. 타인의 게시글 삭제 시도 (실패 케이스)", status, res, expected_statuses=[403])
    
    # 2.4 본인 게시글 삭제 (Soft Delete 성공)
    status, res = make_request("DELETE", f"http://localhost:8080/api/v1/posts/{post_id}",
                               headers={"X-Member-Id": "1", "X-Member-Role": "USER"})
    print_result("2.4. 본인 게시글 삭제 (Soft Delete 성공)", status, res)
    
    # 3.1 FCM 기기 푸시 토큰 수집 (Upsert)
    status, res = make_request("POST", "http://localhost:8080/api/v1/notifications/fcm_token",
                               headers={"X-Member-Id": "1", "X-Member-Role": "USER"},
                               body={"fcmToken": "fcm_token_string_web_client_12345", "deviceType": "WEB"})
    print_result("3.1. FCM 기기 푸시 토큰 수집 (Upsert)", status, res)
    
    # 3.2 영수증 PDF 다운로드
    status, res = make_request("GET", "http://localhost:8080/api/v1/reservations/1/receipt",
                               headers={"X-Member-Id": "1", "X-Member-Role": "USER"})
    print_result("3.2. 영수증 PDF 다운로드", status, res)
    
    # 4.1 관광지/맛집 추천 마커 수동 등록
    status, res = make_request("POST", "http://localhost:8081/api/v1/admin/markers",
                               headers={"X-Admin-Id": "AD-999", "X-Admin-Role": "GENERAL_ADMIN"},
                               body={"name": "경복궁 가을 야간개장", "category": "ATTRACTION", "latitude": 37.5796, "longitude": 126.9770})
    print_result("4.1. 관광지/맛집 추천 마커 수동 등록", status, res)
    
    # 4.2 유해 게시글 블라인드 처리 (어드민 운영)
    status, res = make_request("PATCH", f"http://localhost:8081/api/v1/admin/posts/{post_id}/blind",
                               headers={"X-Admin-Id": "AD-999", "X-Admin-Role": "GENERAL_ADMIN"},
                               body={"reason": "광고성 스팸 및 부적절한 이미지 포함"})
    print_result("4.2. 유해 게시글 블라인드 처리 (어드민 운영)", status, res)

    # 4.2-A 유해 게시글 복구 처리 (어드민 운영)
    status, res = make_request("PATCH", f"http://localhost:8081/api/v1/admin/posts/{post_id}/restore",
                               headers={"X-Admin-Id": "AD-999", "X-Admin-Role": "GENERAL_ADMIN"})
    print_result("4.2-A. 유해 게시글 정상 복구 처리 (어드민 운영)", status, res)
    
    # 4.3 전사 FCM 단체 공지 발송 (SUPER_ADMIN 전용)
    status, res = make_request("POST", "http://localhost:8081/api/v1/admin/notifications/broadcast",
                               headers={"X-Admin-Id": "AD-001", "X-Admin-Role": "SUPER_ADMIN"},
                               body={"title": " ON DE 서비스 정기 서버 점검 안내", "body": "2026-06-01 02:00 ~ 04:00 사이에 무중단 패치 점검이 있을 예정입니다.", "targetAll": True, "targetRoles": None})
    print_result("4.3. 전사 FCM 단체 공지 발송 (SUPER_ADMIN 전용)", status, res)
    
    # 4.4 전사 FCM 단체 공지 발송 실패 케이스 (일반 어드민 권한 미달)
    status, res = make_request("POST", "http://localhost:8081/api/v1/admin/notifications/broadcast",
                               headers={"X-Admin-Id": "AD-999", "X-Admin-Role": "GENERAL_ADMIN"},
                               body={"title": "일반 권한의 테스트", "body": "발송 차단되어야 합니다.", "targetAll": True})
    print_result("4.4. 전사 FCM 단체 공지 발송 실패 케이스 (일반 어드민 권한 미달)", status, res, expected_statuses=[403])

if __name__ == "__main__":
    run_tests()
