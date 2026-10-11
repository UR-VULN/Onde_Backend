import urllib.request
import urllib.error
import json
import time

def make_request(method, url, headers=None, body=None, is_multipart=False):
    if headers is None:
        headers = {}
    
    req_body = None
    if body is not None:
        if isinstance(body, bytes):
            req_body = body
        elif is_multipart:
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
            try:
                return status, json.loads(res.read().decode('utf-8'))
            except Exception:
                return status, "Non-JSON Response"
    except urllib.error.HTTPError as e:
        status = e.code
        try:
            return status, json.loads(e.read().decode('utf-8'))
        except Exception:
            return status, str(e)
    except Exception as e:
        return 500, str(e)

def seed_data():
    print("[START] ONDE Mock Data Seeder is starting...")
    print("Wait 2 seconds...")
    time.sleep(2)

    # 생성된 동적 ID 매핑 보관용 딕셔너리
    member_map = {}

    # 1. 테스트 회원 주입 (USER)
    users = [
        {"id": 1, "name": "홍길동", "role": "USER"},
        {"id": 10, "name": "이순신", "role": "USER"},
        {"id": 11, "name": "유관순", "role": "USER"}
    ]
    for user in users:
        status, res = make_request("POST", "http://localhost:8080/api/v1/members/test", body=user)
        if status == 200 and isinstance(res, dict) and 'data' in res and 'id' in res['data']:
            actual_id = res['data']['id']
            member_map[user['id']] = actual_id
            print(f"--> 회원 생성 [{user['name']}]: Status {status} (요청 ID: {user['id']} -> 할당 ID: {actual_id})")
        else:
            print(f"--> 회원 생성 [{user['name']}] 실패: Status {status}, {res}")

    # 2. 테스트 판매자 주입 (SELLER)
    sellers = [
        {"id": 2, "name": "김판매", "role": "SELLER"},
        {"id": 20, "name": "박셀러", "role": "SELLER"}
    ]
    for seller in sellers:
        status, res = make_request("POST", "http://localhost:8080/api/v1/members/test", body=seller)
        if status == 200 and isinstance(res, dict) and 'data' in res and 'id' in res['data']:
            actual_id = res['data']['id']
            member_map[seller['id']] = actual_id
            print(f"--> 판매자 생성 [{seller['name']}]: Status {status} (요청 ID: {seller['id']} -> 할당 ID: {actual_id})")
        else:
            print(f"--> 판매자 생성 [{seller['name']}] 실패: Status {status}, {res}")

    # 매핑 보완: 만약 매칭되지 않았다면 요청값으로 백업
    for req_id in [1, 2, 10, 11, 20]:
        if req_id not in member_map:
            member_map[req_id] = req_id

    # 3. 테스트 매물 좌표 주입 (LBS - Property)
    properties = [
        {"addressName": "서울시 강남구 테헤란로 1", "latitude": 37.5665, "longitude": 126.9781, "reqSellerId": 2},
        {"addressName": "부산시 해운대구 우동 10", "latitude": 35.1587, "longitude": 129.1604, "reqSellerId": 2},
        {"addressName": "제주도 제주시 노형동 2", "latitude": 33.4996, "longitude": 126.5312, "reqSellerId": 20}
    ]
    for prop in properties:
        db_seller_id = member_map.get(prop["reqSellerId"], prop["reqSellerId"])
        status, res = make_request("POST", "http://localhost:8080/api/v1/properties",
                                   headers={"X-Member-Id": str(db_seller_id), "X-Member-Role": "SELLER"},
                                   body={"addressName": prop["addressName"], "latitude": prop["latitude"], "longitude": prop["longitude"]})
        print(f"--> 매물 주입 [{prop['addressName']}]: Status {status} (Seller ID: {db_seller_id})")

    # 4. 테스트 어드민 가이드 마커 주입 (GuideMarker)
    markers = [
        {"name": "경복궁 가을 야간개장", "category": "ATTRACTION", "latitude": 37.5796, "longitude": 126.9770},
        {"name": "광안대교 오션뷰 카페", "category": "RESTAURANT", "latitude": 35.1536, "longitude": 129.1248},
        {"name": "명동 한우 명가", "category": "RESTAURANT", "latitude": 37.5635, "longitude": 126.9810}
    ]
    for marker in markers:
        status, res = make_request("POST", "http://localhost:8081/api/v1/admin/markers",
                                   headers={"X-Admin-Id": "AD-999", "X-Admin-Role": "GENERAL_ADMIN"},
                                   body=marker)
        print(f"--> 어드민 마커 주입 [{marker['name']}]: Status {status}")

    # 5. 테스트 커뮤니티 피드 주입 (Post)
    boundary = "boundary"
    posts = [
        {
            "reqMemberId": 1,
            "body": (
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="title"\r\n\r\n'
                "도쿄 3박 4일 감성 여행 후기\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="content"\r\n\r\n'
                "신주쿠 라멘 맛집과 감성 골목 위주로 돌았습니다. 대만족이에요!\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="type"\r\n\r\n'
                "REVIEW\r\n"
                f"--{boundary}--\r\n"
            )
        },
        {
            "reqMemberId": 10,
            "body": (
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="title"\r\n\r\n'
                "해운대 송정 서핑 초보 강습 추천\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="content"\r\n\r\n'
                "파도가 적당해서 초보가 서핑 배우기 딱 좋은 시즌입니다. 강사님 친절하네요.\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="type"\r\n\r\n'
                "REVIEW\r\n"
                f"--{boundary}--\r\n"
            )
        },
        {
            "reqMemberId": 11,
            "body": (
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="title"\r\n\r\n'
                "제주 현지인 흑돼지 맛집 알려주세요\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="content"\r\n\r\n'
                "광고글 말고 진짜 제주 도민들이 가시는 찐 흑돼지 단골집 댓글로 추천 부탁드립니다.\r\n"
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="type"\r\n\r\n'
                "COMPANION\r\n"
                f"--{boundary}--\r\n"
            )
        }
    ]
    
    for post in posts:
        db_member_id = member_map.get(post["reqMemberId"], post["reqMemberId"])
        status, res = make_request("POST", "http://localhost:8080/api/v1/posts",
                                   headers={
                                       "X-Member-Id": str(db_member_id),
                                       "X-Member-Role": "USER",
                                       "Content-Type": f"multipart/form-data; boundary={boundary}"
                                   },
                                   body=post["body"].encode('utf-8'))
        print(f"--> 피드 글 주입 [작성자 ID: {db_member_id}]: Status {status}")

    # 6. FCM 가상 디바이스 토큰 데이터 등록 (3.1단계 연계)
    fcm_tokens = [
        {"reqMemberId": 1, "fcmToken": "fcm_token_web_hong_123", "deviceType": "WEB"},
        {"reqMemberId": 10, "fcmToken": "fcm_token_ios_lee_456", "deviceType": "IOS"},
        {"reqMemberId": 11, "fcmToken": "fcm_token_android_yoo_789", "deviceType": "ANDROID"}
    ]
    for token in fcm_tokens:
        db_member_id = member_map.get(token["reqMemberId"], token["reqMemberId"])
        status, res = make_request("POST", "http://localhost:8080/api/v1/notifications/fcm_token",
                                   headers={"X-Member-Id": str(db_member_id), "X-Member-Role": "USER"},
                                   body={"fcmToken": token["fcmToken"], "deviceType": token["deviceType"]})
        print(f"--> FCM 기기등록 [{token['fcmToken']}](Member ID: {db_member_id}): Status {status}")

    print("\n[SUCCESS] Mock Data Seeding Completed Successfully with Dynamic Sequence Mapping!")

if __name__ == "__main__":
    seed_data()
