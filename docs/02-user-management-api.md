# User Server PRD - 사용자 관리 API

## 1. 사용자 관리 API

### 1.1 사용자 목록 조회 (List)
관리자와 시스템 서비스가 사용자 풀을 탐색할 수 있도록 페이지네이션·필터·정렬을 지원한다.

```http
GET /users?page=0&size=20&keyword=yuuki&sort=createdAt,desc
user-id: {adminUserId}
role: ADMIN
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `page` | int | 옵션 (기본 0) | 조회 페이지 번호 |
| `size` | int | 옵션 (기본 20, 최대 100) | 페이지 당 사용자 수 |
| `role` | enum(USER, ADMIN) | 헤더 | 권한별 필터. `role` 헤더로 전달 |
| `keyword` | string | 옵션 | 이름/이메일 like 검색 |
| `createdFrom`, `createdTo` | ISO8601 datetime | 옵션 | 가입 기간 필터 |
| `sort` | string | 옵션 (기본 `createdAt,desc`) | `필드,방향` 형식의 정렬 기준 |

**응답:**
```http
HTTP/1.1 200 OK
{
    "status": "success",
    "message": "list users",
    "data": {
        "content": [
            {
                "userId": "string",
                "email": "string",
                "name": "string",
                "role": "USER|ADMIN",
                "remainToken": 10000,
                "profile": "https://...",
                "createdAt": "2024-01-01T00:00:00"
            }
        ],
        "page": 0,
        "size": 20,
        "totalElements": 120,
        "totalPages": 6,
        "totalUserCount": 532
    }
}
```

**요구사항**
- 권한: ADMIN 전용. USER는 자신의 정보만 `/users/profile`로 접근.
- 정렬 필드는 `createdAt`, `name`, `remainToken`만 허용.
- 권한 필터링 시 `role` 헤더 값만 참조하며, 미전달 시 전체 조회.
- 응답에는 현재 조건에 맞는 페이지 정보 외에 전체 사용자 수(`totalUserCount`)도 포함하여 대시보드 지표로 활용.
- 1초 내 응답을 위해 인덱스(`role`, `created_at`) 활용 및 N+1 쿼리 금지.

### 1.2 사용자 상세 조회 (Read)
```http
GET /users/profile
user-id: {userId}
```

**응답:**
```http
HTTP/1.1 200 OK
{
    "status": "success",
    "message": "find user",
    "data": {
        "userId": "string",
        "email": "string",
        "name": "string",
        "role": "USER|ADMIN",
        "remainToken": 10000,
        "profile": "https://...",
        "createdAt": "2024-01-01T00:00:00"
    }
}
```

### 1.3 관리자용 사용자 프로필 조회 (Admin Read)
관리자가 특정 사용자 ID로 상세 정보를 열람할 수 있는 별도 엔드포인트다.

```http
GET /users/admin/{userId}
user-id: {userId}
role: ADMIN
```

**인가 요구사항**
- API Gateway가 JWT를 검증하고 `user-id`, `role` 헤더를 내려준다.
- `role` 헤더가 ADMIN일 때만 200 응답, 아니면 403 Forbidden.
- 감사 로그에 `requestAdminId`, `targetUserId`, `requestedAt` 기록.

**응답:** 기본적으로 `/users/profile`과 동일한 `UserResponse` 페이로드.

### 1.4 사용자 일괄 조회 by ID
```http
GET /users?userIds=id1,id2,id3
user-id: {serviceAccountId}
```

**요구사항**
- `userIds` 최대 50개, 존재하지 않는 ID는 제외하고 `notFoundUserIds` 배열로 응답.
- 내부 서비스(gRPC 또는 REST)에서 캐싱 용도로 사용.

### 1.5 사용자 등록 (Create)
```http
POST /users/register
Content-Type: application/json

{
    "userId": "string",
    "email": "string",
    "name": "string",
    "password": "string"
}
```

**응답:**
```http
HTTP/1.1 201 Created
{
    "status": "success",
    "message": "register user",
    "data": null
}
```

**validation**
- `userId`: 6~32자, 영문/숫자/하이픈 조합.
- `password`: 최소 8자, 대소문자/숫자/특수문자 포함.
- 이메일 중복 시 `409 Conflict` 반환.

### 1.6 관리자 등록 (Create Admin)
```http
POST /users/register/admin
Content-Type: application/json
user-id: {adminUserId}

{
    "userId": "string",
    "email": "string",
    "name": "string",
    "password": "string"
}
```

**요구사항**
- 요청자는 ADMIN 이어야 하며, 사용자 수 1,000명 이상일 때 2FA 로그 기록.

### 1.7 사용자 정보 수정 (Update)

#### 1.6.1 프로필 이미지 변경
```http
PATCH /users/change/profile
Content-Type: multipart/form-data
user-id: {userId}

profile: (file)
```

#### 1.6.2 사용자 이름 변경
```http
PATCH /users/name
Content-Type: application/json
user-id: {userId}

{
    "name": "string"
}
```

**공통 validation**
- 요청자는 본인 또는 ADMIN.
- 프로필 파일: JPG/PNG, 5MB 이하. 업로드 실패 시 롤백.
- 이름: 2~20자, 이모지/특수문자 금지.

### 1.8 사용자 삭제 (Delete)
```http
DELETE /users
user-id: {userId}
role: USER|ADMIN

{
    "userId": "targetUserId"   // ADMIN 전용, 생략 시 본인 삭제
}
```

**요구사항**
- 본인 삭제 또는 ADMIN이 탈퇴 처리 가능.
- body에 `userId`가 없는 경우 `user-id` 헤더의 본인을 삭제하며, 타겟 지정 시 ADMIN 권한 필수.
- 삭제 시 S3 프로필, 토큰 레코드, 세션 캐시 삭제.
- 불변 로그 테이블에 `userId`, `deletedBy`, `deletedAt` 저장.

## 2. 사용자 복구 API

### 2.1 비밀번호 재설정
```http
PATCH /users/retrieve/password
Content-Type: application/json

{
    "email": "string",
    "password": "string"
}
```

### 2.2 사용자 ID 찾기
```http
POST /users/retrieve/userId
Content-Type: application/json

{
    "email": "string",
    "password": "string"
}
```

## 3. API 응답 코드 정의

| HTTP Code | 설명 | 사용 사례 |
|-----------|------|-----------|
| 200 | OK | 정상 조회, 수정 |
| 201 | Created | 사용자 생성 |
| 202 | Accepted | 비동기 처리 시작 |
| 400 | Bad Request | 잘못된 요청 파라미터 |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 사용자 미존재 |
| 409 | Conflict | 중복 데이터 |
| 500 | Internal Server Error | 시스템 오류 |
