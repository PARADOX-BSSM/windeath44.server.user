# User Server PRD - 사용자 관리 API

## 1. 사용자 관리 API

### 1.1 사용자 등록
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

### 1.2 관리자 등록
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

### 1.3 사용자 프로필 조회
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

### 1.4 사용자 목록 조회
```http
GET /users?userIds=id1,id2,id3
```

### 1.5 프로필 이미지 변경
```http
PATCH /users/change/profile
Content-Type: multipart/form-data
user-id: {userId}

profile: (file)
```

### 1.6 사용자 이름 변경
```http
PATCH /users/name
Content-Type: application/json
user-id: {userId}

{
    "name": "string"
}
```

### 1.7 사용자 삭제
```http
DELETE /users
user-id: {userId}
```

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